package io.github.danthe1st.httpsintercept.config;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

public record HostMatcher(
		Set<String> exactHosts,
		Set<String> hostParts,
		Set<Pattern> hostRegexes) {
	
	public HostMatcher(Set<String> exactHosts, Set<String> hostParts, Set<Pattern> hostRegexes) {
		this.exactHosts = copyOrEmptyIfNull(exactHosts);
		this.hostParts = copyOrEmptyIfNull(hostParts);
		this.hostRegexes = copyOrEmptyIfNull(hostRegexes);
	}
	
	private <T> Set<T> copyOrEmptyIfNull(Set<T> data) {
		if(data == null){
			return Collections.emptySet();
		}
		return Set.copyOf(data);
	}
	
	public boolean matches(String hostname) {
		return exactHosts.contains(hostname) || doesMatchPart(hostname) || matchesRegex(hostname);
	}
	
	private boolean matchesRegex(String hostname) {
		for(Pattern pattern : hostRegexes){
			if(pattern.matcher(hostname).matches()){
				return true;
			}
		}
		return false;
	}
	
	private boolean doesMatchPart(String hostname) {
		if(hostParts.isEmpty()){
			return false;
		}
		
		int index = 0;
		do{
			if(hostParts.contains(hostname.substring(index))){
				return true;
			}
		}while((index = hostname.indexOf('.', index) + 1) != 0 && index < hostname.length());
		
		return false;
	}
}
