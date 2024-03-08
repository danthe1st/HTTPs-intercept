package io.github.danthe1st.httpsintercept.handler;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.danthe1st.httpsintercept.config.Config;
import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import io.github.danthe1st.httpsintercept.handler.http.IncomingHttpRequestHandler;
import io.github.danthe1st.httpsintercept.handler.sni.CustomSniHandler;
import io.github.danthe1st.httpsintercept.handler.sni.SNIHandlerMapping;
import io.github.danthe1st.httpsintercept.matcher.IterativeHostMatcher;
import io.github.danthe1st.httpsintercept.rules.PreForwardRule;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SniHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandlersInit extends ChannelInitializer<SocketChannel> {
	
	static final Logger LOG = LoggerFactory.getLogger(ServerHandlersInit.class);
	
	private final Bootstrap clientBootstrapTemplate;
	private final SNIHandlerMapping sniMapping;
	private final Config config;
	
	private IterativeHostMatcher<PreForwardRule> preForwardMatcher;
	
	public ServerHandlersInit(Bootstrap clientBootstrap, Config config) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		this.clientBootstrapTemplate = clientBootstrap;
		sniMapping = SNIHandlerMapping.createMapping();
		this.config = config;
		
		List<PreForwardRule> preForwardRules = config.preForwardRules();
		List<Map.Entry<HostMatcherConfig, PreForwardRule>> rules = new ArrayList<>();
		for(PreForwardRule preForwardRule : preForwardRules){
			HostMatcherConfig hostMatcher = preForwardRule.hostMatcher();
			if(hostMatcher == null){
				hostMatcher = new HostMatcherConfig(null, null, null);
			}
			rules.add(Map.entry(hostMatcher, preForwardRule));
		}
		preForwardMatcher = new IterativeHostMatcher<>(rules);
	}
	
	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		SniHandler sniHandler = new CustomSniHandler(sniMapping, clientBootstrapTemplate, config);
		socketChannel.pipeline().addLast(
				sniHandler,
				new HttpServerCodec(),
				new HttpObjectAggregator(1048576),
				new IncomingHttpRequestHandler(sniHandler, clientBootstrapTemplate, preForwardMatcher)
		);
	}
}
