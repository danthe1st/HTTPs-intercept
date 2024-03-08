package io.github.danthe1st.httpsintercept.rules;

import java.util.Map;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SetHeaderRule(HostMatcherConfig hostMatcher,
		Map<String, String> headers)
		implements PreForwardRule {
	
	private static final Logger LOG = LoggerFactory.getLogger(SetHeaderRule.class);
	
	@Override
	public boolean processRequest(FullHttpRequest fullHttpRequest, Channel channel) {
		LOG.debug("add headers: {}", headers);
		headers.forEach((name, value) -> fullHttpRequest.headers().set(name, value));
		return true;
	}
}
