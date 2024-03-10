package io.github.danthe1st.httpsintercept.config;

import java.util.Collections;
import java.util.Set;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public record HostMatcherConfig(Set<String> exact,
		Set<String> partial,
		Set<String> regex) {
	
	public HostMatcherConfig(@Nullable Set<String> exact, @Nullable Set<String> partial, @Nullable Set<String> regex) {
		this.exact = emptyIfNull(exact);
		this.partial = emptyIfNull(partial);
		this.regex = emptyIfNull(regex);
	}
	
	private Set<String> emptyIfNull(@UnderInitialization HostMatcherConfig this, @Nullable Set<String> data) {
		if(data == null){
			return Collections.emptySet();
		}
		return Set.copyOf(data);
	}
	
}
