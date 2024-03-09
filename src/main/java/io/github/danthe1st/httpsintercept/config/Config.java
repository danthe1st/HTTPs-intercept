package io.github.danthe1st.httpsintercept.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.danthe1st.httpsintercept.rules.PostForwardRule;
import io.github.danthe1st.httpsintercept.rules.PreForwardRule;

public record Config(
		HostMatcherConfig ignoredHosts,
		List<PreForwardRule> preForwardRules,
		List<PostForwardRule> postForwardRules
) {
	
	public Config(HostMatcherConfig ignoredHosts, List<PreForwardRule> preForwardRules, List<PostForwardRule> postForwardRules) {
		if(ignoredHosts == null){
			ignoredHosts = new HostMatcherConfig(null, null, null);
		}
		this.ignoredHosts = ignoredHosts;
		this.preForwardRules = emptyIfNull(preForwardRules);
		this.postForwardRules = emptyIfNull(postForwardRules);
	}
	
	private <T> List<T> emptyIfNull(List<T> preForwardRules) {
		if(preForwardRules == null){
			return Collections.emptyList();
		}
		return List.copyOf(preForwardRules);
	}
	
	public static Config load(Path path) throws IOException {
		if(!Files.exists(path)) {
			return new Config(null, null, null);
		}
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try(Reader reader = Files.newBufferedReader(path)){
			return mapper.readValue(reader, Config.class);
		}
	}
}
