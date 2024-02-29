package io.github.danthe1st.httpsintercept.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostMatcher {
	
	private static final Logger LOG = LoggerFactory.getLogger(HostMatcher.class);
	
	private final Set<String> exactHosts;
	private final Set<String> hostParts;
	private List<Pattern> hostRegexes;
	
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
		List<Pattern> hostRegexes = new ArrayList<>();
		for(String declaration : hostDeclarations){
			processHostDeclaration(declaration, exactHosts, hostParts, hostRegexes);
		}
		return new HostMatcher(exactHosts, hostParts, hostRegexes);
	}

	private static void processHostDeclaration(String declaration, Set<String> exactHosts, Set<String> hostParts, List<Pattern> hostRegexes) {
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
	
	HostMatcher(Set<String> exactHosts, Set<String> hostParts, List<Pattern> hostRegexes) {
		this.exactHosts = Set.copyOf(exactHosts);
		this.hostParts = Set.copyOf(hostParts);
		this.hostRegexes = List.copyOf(hostRegexes);
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
	
	Set<String> getExactHosts() {
		return Collections.unmodifiableSet(exactHosts);
	}
	
	Set<String> getHostParts() {
		return Collections.unmodifiableSet(hostParts);
	}
	
	List<Pattern> getHostRegexes() {
		return Collections.unmodifiableList(hostRegexes);
	}
}
