package io.github.danthe1st.httpsintercept.config;

import java.util.Collections;
import java.util.Set;

public record HostMatcherConfig(Set<String> exactHosts,
		Set<String> hostParts,
		Set<String> hostRegexes) {
	
	public HostMatcherConfig(Set<String> exactHosts, Set<String> hostParts, Set<String> hostRegexes) {
		this.exactHosts = emptyIfNull(exactHosts);
		this.hostParts = emptyIfNull(hostParts);
		this.hostRegexes = emptyIfNull(hostRegexes);
	}
	
	private Set<String> emptyIfNull(Set<String> data) {
		if(data == null){
			return Collections.emptySet();
		}
		return Set.copyOf(data);
	}
	
}
