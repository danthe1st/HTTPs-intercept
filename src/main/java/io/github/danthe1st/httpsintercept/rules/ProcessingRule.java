package io.github.danthe1st.httpsintercept.rules;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;

public interface ProcessingRule {
	HostMatcherConfig hostMatcher();
}
