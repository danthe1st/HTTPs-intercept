package io.github.danthe1st.httpsintercept.handler.sni;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import io.github.danthe1st.httpsintercept.handler.raw.RawForwardIncomingRequestHandler;
import io.github.danthe1st.httpsintercept.matcher.HostMatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSniHandler extends SniHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(CustomSniHandler.class);
	
	private final Bootstrap clientBootstrapTemplate;
	
	private final HostMatcher<Object> ignoredHosts;
	
	public CustomSniHandler(Mapping<? super String, ? extends SslContext> mapping, Bootstrap clientBootstrapTemplate, HostMatcher<Object> ignoredHostMatcher) throws IOException {
		super(mapping);
		this.clientBootstrapTemplate = clientBootstrapTemplate;
		ignoredHosts = ignoredHostMatcher;
	}
	
	@Override
	protected void replaceHandler(ChannelHandlerContext channelHandlerContext, String hostname, SslContext sslContext) throws Exception {
		ChannelPipeline pipeline = channelHandlerContext.pipeline();
		// TODO no hostname
		if(ignoredHosts.allMatches(hostname).hasNext()){
			LOG.info("skipping hostname {}", hostname);
			
			boolean foundThis = false;
			
			for(Iterator<Map.Entry<String, ChannelHandler>> it = pipeline.iterator(); it.hasNext();){
				ChannelHandler handler = it.next().getValue();
				if(foundThis){
					it.remove();
				}
				if(handler == this){
					foundThis = true;
				}
			}
			
			if(!foundThis){
				throw new IllegalStateException("cannot find self handler in pipeline");
			}
			pipeline.replace(this, "forwardNoProcess", new RawForwardIncomingRequestHandler(hostname, clientBootstrapTemplate));
		}else{
			super.replaceHandler(channelHandlerContext, hostname, sslContext);
		}
	}
}
