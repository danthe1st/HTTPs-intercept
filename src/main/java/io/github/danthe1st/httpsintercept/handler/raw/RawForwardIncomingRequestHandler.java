package io.github.danthe1st.httpsintercept.handler.raw;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for requests that need to be forwarded to the requested server.
 * This class accepts raw SSL/TLS requests that should be forwarded without decoding/re-encoding.
 * It sends a request to the target server which is then processed by {@link RawForwardedOutgoingRequestHandler}.
 */
public final class RawForwardIncomingRequestHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger LOG = LoggerFactory.getLogger(RawForwardIncomingRequestHandler.class);
	
	private final String hostname;
	private final Bootstrap clientBootstrapTemplate;
	private Channel outChannel = null;
	
	public RawForwardIncomingRequestHandler(String hostname, Bootstrap clientBootstrapTemplate) {
		this.hostname = hostname;
		this.clientBootstrapTemplate = clientBootstrapTemplate;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
		if(outChannel == null){
			Bootstrap actualClientBootstrap = clientBootstrapTemplate.clone()
				.handler(new RawForwardedOutgoingRequestHandler(ctx));
			try{
				outChannel = actualClientBootstrap.connect(hostname, 443)
					.sync()
					.channel();
			}catch(RuntimeException e){
				// this can happen due to internet connection issues or something on the remove side
				// cannot do much more than aborting the connection
				// because this is before a TLS connection is established (when the client hello packet is read)
				LOG.error("An exception occured trying to establish a raw connection for forwarding", e);
				ctx.channel().close();
				return;
			}
		}
		
		outChannel.writeAndFlush(msg).sync();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		if(outChannel != null){
			outChannel.close();
		}
		ctx.channel().close();
	}
}