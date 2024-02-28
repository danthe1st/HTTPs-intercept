package io.github.danthe1st.httpsintercept.handler.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requests need to be forwarded to the requested server.
 * This class encodes the forwarded request, decodes the response and sends it back
 */
final class OutgoingHttpRequestHandler extends ChannelInitializer<SocketChannel> {
	private static final Logger LOG = LoggerFactory.getLogger(OutgoingHttpRequestHandler.class);
	
	private final ChannelHandlerContext originalClientContext;
	private final SslContext forwardSslContext;
	
	OutgoingHttpRequestHandler(ChannelHandlerContext originalClientContext, SslContext clientSslContext) {
		this.originalClientContext = originalClientContext;
		this.forwardSslContext = clientSslContext;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		p.addLast(
				forwardSslContext.newHandler(ch.alloc()), // use SSL/TLS for the outgoing request
				new HttpClientCodec(), // encode/decode HTTP
				new ResponseHandler()
		);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error("An exception occured while forwarding a request", cause);
		originalClientContext.channel().close();
		ctx.channel().close();
	}
	
	private final class ResponseHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			LOG.debug("read: {}", msg);
			originalClientContext.writeAndFlush(msg);
			
			if(msg instanceof LastHttpContent){
				LOG.debug("last HTTP content");
				originalClientContext.channel().close();
				ctx.channel().close();
			}
		}
		
		@Override
		public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
			LOG.debug("channel unregistered");
			originalClientContext.channel().close();
			ctx.channel().close();
			super.channelUnregistered(ctx);
		}
	}
}