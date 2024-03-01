package io.github.danthe1st.httpsintercept.handler.raw;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for requests that are forwarded to the requested server without decoding/re-encoding.
 * @see RawForwardIncomingRequestHandler
 */
public final class RawForwardedOutgoingRequestHandler extends ChannelInitializer<SocketChannel> {
	private static final Logger LOG = LoggerFactory.getLogger(RawForwardedOutgoingRequestHandler.class);
	
	private final ChannelHandlerContext originalClientContext;
	
	public RawForwardedOutgoingRequestHandler(ChannelHandlerContext originalClientContext) {
		this.originalClientContext = originalClientContext;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		p.addLast(new ResponseHandler());
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error("An exception occured while forwarding a request", cause);
		originalClientContext.channel().close();
		ctx.channel().close();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		originalClientContext.channel().close();
		ctx.channel().close();
	}
	
	private final class ResponseHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			LOG.debug("read: {}", msg);
			originalClientContext.writeAndFlush(msg);
		}
		
		@Override
		public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
			originalClientContext.channel().close();
			ctx.channel().close();
			super.channelUnregistered(ctx);
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			LOG.error("An exception occured while forwarding a raw request", cause);
			originalClientContext.channel().close();
			ctx.channel().close();
		}
	}
}