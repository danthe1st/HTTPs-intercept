package io.github.danthe1st.httpsintercept.config;

import java.util.Objects;
import java.util.Set;

public record HostMatcherConfig(Set<String> exactHosts,
		Set<String> hostParts,
		Set<String> hostRegexes) {
	public HostMatcherConfig {
		Objects.requireNonNull(exactHosts);
		Objects.requireNonNull(hostParts);
		Objects.requireNonNull(hostRegexes);
	}
	
}
