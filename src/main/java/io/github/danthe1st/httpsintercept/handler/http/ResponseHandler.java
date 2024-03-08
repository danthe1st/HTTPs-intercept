package io.github.danthe1st.httpsintercept.handler.http;

import io.github.danthe1st.httpsintercept.rules.PostForwardRule;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the response of the outgoing/forwarded request
 */
final class ResponseHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);
	
	private final ChannelHandlerContext originalClientContext;
	private final Iterable<PostForwardRule> postForwardRules;
	
	private final FullHttpRequest fullHttpRequest;
	
	public ResponseHandler(ChannelHandlerContext originalClientContext, FullHttpRequest fullHttpRequest, Iterable<PostForwardRule> postForwardRules) {
		this.originalClientContext = originalClientContext;
		this.fullHttpRequest = fullHttpRequest;
		this.postForwardRules = postForwardRules;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		LOG.debug("read: {}", msg);
		
		if(msg instanceof FullHttpResponse res){
			processRules(res);
		}else{
			LOG.error("respose message not assignable to FullHttpResponse: {}", msg);
			originalClientContext.writeAndFlush(msg);
		}
		
		originalClientContext.channel().close();
		ctx.channel().close();
	}

	private void processRules(FullHttpResponse res) {
		HttpResponseContentAccessor contentAccessor = new HttpResponseContentAccessor(res);
		for(PostForwardRule rule : postForwardRules){
			if(!rule.processRequest(fullHttpRequest, res, contentAccessor, originalClientContext.channel())){
				return;
			}
		}
		originalClientContext.writeAndFlush(res);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		LOG.debug("channel unregistered");
		originalClientContext.channel().close();
		ctx.channel().close();
		super.channelUnregistered(ctx);
	}
}