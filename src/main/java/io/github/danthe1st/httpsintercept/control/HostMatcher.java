package io.github.danthe1st.httpsintercept.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HostMatcher {
	private Set<String> exactHosts = new HashSet<>();
	private Set<String> hostParts = new HashSet<>();
	
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
		for(String ignored : hostDeclarations){
			if(ignored.startsWith("*.")){
				hostParts.add(ignored.substring(2));
			}else{
				exactHosts.add(ignored);
			}
		}
		return new HostMatcher(exactHosts, hostParts);
	}
	
	HostMatcher(Set<String> exactHosts, Set<String> hostParts) {
		this.exactHosts = Set.copyOf(exactHosts);
		this.hostParts = Set.copyOf(hostParts);
	}
	
	public boolean matches(String hostname) {
		return exactHosts.contains(hostname) || doesMatchPart(hostname);
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
}
