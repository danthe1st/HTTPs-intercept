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
import io.github.danthe1st.httpsintercept.rules.PostForwardRule;
import io.github.danthe1st.httpsintercept.rules.PreForwardRule;
import io.github.danthe1st.httpsintercept.rules.ProcessingRule;
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
	
	private final IterativeHostMatcher<Object> ignoredHostMatcher;
	private final IterativeHostMatcher<PreForwardRule> preForwardMatcher;
	private final IterativeHostMatcher<PostForwardRule> postForwardMatcher;
	
	public ServerHandlersInit(Bootstrap clientBootstrap, Config config) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		this.clientBootstrapTemplate = clientBootstrap;
		sniMapping = SNIHandlerMapping.createMapping();
		
		preForwardMatcher = createMatcherFromRules(config.preForwardRules());
		postForwardMatcher = createMatcherFromRules(config.postForwardRules());
		ignoredHostMatcher = new IterativeHostMatcher<>(List.of(Map.entry(config.ignoredHosts(), new Object())));
	}

	private <T extends ProcessingRule> IterativeHostMatcher<T> createMatcherFromRules(List<T> ruleList) {
		List<Map.Entry<HostMatcherConfig, T>> rules = new ArrayList<>();
		for(T rule : ruleList){
			HostMatcherConfig hostMatcher = rule.hostMatcher();
			if(hostMatcher == null){
				hostMatcher = new HostMatcherConfig(null, null, null);
			}
			rules.add(Map.entry(hostMatcher, rule));
		}
		return new IterativeHostMatcher<>(rules);
	}
	
	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		SniHandler sniHandler = new CustomSniHandler(sniMapping, clientBootstrapTemplate, ignoredHostMatcher);
		socketChannel.pipeline().addLast(
				sniHandler,
				new HttpServerCodec(),
				new HttpObjectAggregator(Integer.MAX_VALUE),
				new IncomingHttpRequestHandler(sniHandler, clientBootstrapTemplate, preForwardMatcher, postForwardMatcher)
		);
	}
}
