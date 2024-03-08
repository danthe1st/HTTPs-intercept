package io.github.danthe1st.httpsintercept.config;

import java.util.Collections;
import java.util.Set;

public record HostMatcherConfig(Set<String> exact,
		Set<String> partial,
		Set<String> regex) {
	
	public HostMatcherConfig(Set<String> exact, Set<String> partial, Set<String> regex) {
		this.exact = emptyIfNull(exact);
		this.partial = emptyIfNull(partial);
		this.regex = emptyIfNull(regex);
	}
	
	private Set<String> emptyIfNull(Set<String> data) {
		if(data == null){
			return Collections.emptySet();
		}
		return Set.copyOf(data);
	}
	
}
