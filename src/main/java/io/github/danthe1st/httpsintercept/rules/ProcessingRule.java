package io.github.danthe1st.httpsintercept.rules;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProcessingRule {
	@Nullable
	HostMatcherConfig hostMatcher();
}
