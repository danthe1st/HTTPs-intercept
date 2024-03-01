package io.github.danthe1st.httpsintercept.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record HostMatcher(
		Set<String> exactHosts,
		Set<String> hostParts,
		Set<Pattern> hostRegexes) {
	
	private static final Logger LOG = LoggerFactory.getLogger(HostMatcher.class);
	
	public static HostMatcher load(Path config) throws IOException {
		if(!Files.exists(config)){
			Files.createFile(config);
			return load(Collections.emptyList());
		}
		List<String> hostDeclarations = Files.readAllLines(config);
		return load(hostDeclarations);
	}
	
	static HostMatcher load(List<String> hostDeclarations) {
		Set<String> exactHosts = new HashSet<>();
		Set<String> hostParts = new HashSet<>();
		Set<Pattern> hostRegexes = new HashSet<>();
		for(String declaration : hostDeclarations){
			processHostDeclaration(declaration, exactHosts, hostParts, hostRegexes);
		}
		return new HostMatcher(exactHosts, hostParts, hostRegexes);
	}

	private static void processHostDeclaration(String declaration, Set<String> exactHosts, Set<String> hostParts, Set<Pattern> hostRegexes) {
		if(declaration.isBlank() || declaration.startsWith("#")){
			return;
		}
		if(declaration.startsWith("/")){
			String hostRegex = declaration.substring(1);
			try{
				hostRegexes.add(Pattern.compile(hostRegex));
			}catch(PatternSyntaxException e){
				LOG.error("invalid regex: {}", hostRegex);
			}
		}else if(declaration.startsWith("*.")){
			hostParts.add(declaration.substring(2));
		}else{
			exactHosts.add(declaration);
		}
	}
	
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
		while((index = hostname.indexOf('.', index) + 1) != 0 && index < hostname.length()){
			if(hostParts.contains(hostname.substring(index))){
				return true;
			}
		}
		
		return false;
	}
}
