package io.github.danthe1st.httpsintercept.handler.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Handles the response of the outgoing/forwarded request
 */
final class ResponseHandler extends ChannelInboundHandlerAdapter {
	
	private final ChannelHandlerContext originalClientContext;
	
	public ResponseHandler(ChannelHandlerContext originalClientContext) {
		this.originalClientContext = originalClientContext;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		OutgoingHttpRequestHandler.LOG.debug("read: {}", msg);
		originalClientContext.writeAndFlush(msg);
		
		if(msg instanceof LastHttpContent){
			OutgoingHttpRequestHandler.LOG.debug("last HTTP content");
			originalClientContext.channel().close();
			ctx.channel().close();
		}
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		OutgoingHttpRequestHandler.LOG.debug("channel unregistered");
		originalClientContext.channel().close();
		ctx.channel().close();
		super.channelUnregistered(ctx);
	}
}