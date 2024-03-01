package io.github.danthe1st.httpsintercept.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public record Config(
		HostMatcher ignoredHosts
) {
	
	public Config {
		Objects.requireNonNull(ignoredHosts);
	}
	
	public static Config load(Path path) throws IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try(Reader reader = Files.newBufferedReader(path)){
			return mapper.readValue(reader, Config.class);
		}
	}
}
