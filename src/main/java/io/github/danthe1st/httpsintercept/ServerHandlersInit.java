package io.github.danthe1st.httpsintercept;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.IllegalReferenceCountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandlersInit extends ChannelInitializer<SocketChannel> {
	
	private static final Logger LOG = LoggerFactory.getLogger(ServerHandlersInit.class);
	
	private final Bootstrap clientBootstrap;
	private final SslContext clientSslContext;
	private final SSLHandlerMapping sniMapping;
	
	public ServerHandlersInit(Bootstrap clientBootstrap) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		this.clientBootstrap = clientBootstrap;
		sniMapping = new SSLHandlerMapping();
		clientSslContext = SslContextBuilder.forClient().build();
	}
	
	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		SniHandler sniHandler = new SniHandler(sniMapping);
		socketChannel.pipeline().addLast(
				sniHandler,
				new HttpServerCodec(),
				new HttpObjectAggregator(1048576),
				new SimpleChannelInboundHandler<FullHttpRequest>() {
					
					@Override
					protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
						Bootstrap actualClientBootstrap = clientBootstrap.clone()
							.handler(
									new ChannelInitializer<SocketChannel>() {
										
										@Override
										protected void initChannel(SocketChannel ch) throws Exception {
											ChannelPipeline p = ch.pipeline();
											p.addLast(clientSslContext.newHandler(ch.alloc()));
											p.addLast(new HttpClientCodec());
											p.addLast(new ChannelInboundHandlerAdapter() {
												@Override
												public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
													LOG.debug("READ: {}", msg);
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
											LOG.error("An exception occured while forwarding a request", cause);
											channelHandlerContext.channel().close();
											ctx.channel().close();
										}
									}
							);
						Channel outChannel = actualClientBootstrap.connect(sniHandler.hostname(), 443)
							.sync().channel();
						
						outChannel.writeAndFlush(fullHttpRequest).sync();
					}
					
					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						if(!(cause instanceof IllegalReferenceCountException)){
							LOG.error("An exception occured trying to process a request", cause);
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
		);
	}
	
}
