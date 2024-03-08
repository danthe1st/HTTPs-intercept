package io.github.danthe1st.httpsintercept.handler.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.github.danthe1st.httpsintercept.matcher.IterativeHostMatcher;
import io.github.danthe1st.httpsintercept.rules.PostForwardRule;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requests need to be forwarded to the requested server.
 * This class encodes the forwarded request, decodes the response and sends it back
 */
final class OutgoingHttpRequestHandler extends ChannelInitializer<SocketChannel> {
	static final Logger LOG = LoggerFactory.getLogger(OutgoingHttpRequestHandler.class);
	
	private final ChannelHandlerContext originalClientContext;
	private final String hostname;
	private final FullHttpRequest fullHttpRequest;
	
	private final IterativeHostMatcher<PostForwardRule> postForwardMatcher;
	
	OutgoingHttpRequestHandler(ChannelHandlerContext originalClientContext, FullHttpRequest fullHttpRequest, String hostname, IterativeHostMatcher<PostForwardRule> postForwardMatcher) {
		this.originalClientContext = originalClientContext;
		this.hostname = hostname;
		this.fullHttpRequest = fullHttpRequest;
		this.postForwardMatcher = postForwardMatcher;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		
		SSLEngine engine = SSLContext.getDefault().createSSLEngine(hostname, 443);
		
		engine.setUseClientMode(true);
		
		p.addLast(
				new SslHandler(engine),
				new HttpClientCodec(), // encode/decode HTTP
				new HttpContentDecompressor(),
				new HttpObjectAggregator(Integer.MAX_VALUE),
				new ResponseHandler(originalClientContext, fullHttpRequest, postForwardMatcher.matchesAsIterable(hostname))
		);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error("An exception occured while forwarding a request", cause);
		originalClientContext.channel().close();
		ctx.channel().close();
	}
}