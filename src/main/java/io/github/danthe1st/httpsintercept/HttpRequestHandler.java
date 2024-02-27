package io.github.danthe1st.httpsintercept;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.IllegalReferenceCountException;

/**
 * Forwards incoming (already decrypted and preprocessed) HTTPs requests to the requested server and sends the response back
 */
final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private final SniHandler sniHandler;
	private final Bootstrap clientBootstrap;
	private final SslContext clientSslContext;
	
	/**
	 * @param sniHandler Netty handler for Server Name Identification (contains the actual target host name)
	 * @param clientSslContext {@link SslContext} used for the outgoing request
	 * @param clientBootstrap template for sending the outgoing request
	 */
	HttpRequestHandler(SniHandler sniHandler, SslContext clientSslContext, Bootstrap clientBootstrap) {
		this.sniHandler = sniHandler;
		this.clientBootstrap = clientBootstrap;
		this.clientSslContext = clientSslContext;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
		Bootstrap actualClientBootstrap = clientBootstrap.clone()
			.handler(
					new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							p.addLast(clientSslContext.newHandler(ch.alloc()));// use SSL/TLS for the outgoing request
							p.addLast(new HttpClientCodec());// encode/decode HTTP
							p.addLast(new ChannelInboundHandlerAdapter() {
								// forward request to client and give back response
								@Override
								public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
									ServerHandlersInit.LOG.debug("READ: {}", msg);
									channelHandlerContext.writeAndFlush(msg);
									
									if(msg instanceof HttpContent){
										channelHandlerContext.channel().close();
										ctx.channel().close();
									}
								}
							});
						}
						
						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							ServerHandlersInit.LOG.error("An exception occured while forwarding a request", cause);
							channelHandlerContext.channel().close();
							ctx.channel().close();
						}
					}
			);
		
		Channel outChannel = actualClientBootstrap.connect(sniHandler.hostname(), 443)
			.sync()
			.channel();
		
		outChannel.writeAndFlush(fullHttpRequest).sync();
	}
	
	// in case Netty caught an exception (e.g. the server is unreachable),
	// the client receives a 502 Bad Gateway response including the stack trace
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(!(cause instanceof IllegalReferenceCountException)){
			ServerHandlersInit.LOG.error("An exception occured trying to process a request", cause);
			Channel channel = ctx.channel();
			try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintStream exceptionStream = new PrintStream(baos)){
				exceptionStream.println("An exception occured trying to intercept the transmission");
				exceptionStream.println();
				cause.printStackTrace(exceptionStream);
				FullHttpResponse response = new DefaultFullHttpResponse(
						HttpVersion.HTTP_1_1,
						HttpResponseStatus.BAD_GATEWAY,
						Unpooled.copiedBuffer(baos.toByteArray())
				);
				channel.writeAndFlush(response).sync();
			}finally{
				channel.close();
			}
		}
		
	}
}