package io.github.danthe1st.httpsintercept.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class HostMatcherTests {
	@Test
	void testExactMatch() {
		HostMatcher matcher = new HostMatcher(
				Set.of("example.com"),
				Collections.emptySet(),
				Collections.emptySet()
		);
		assertTrue(matcher.matches("example.com"));
		assertFalse(matcher.matches("github.com"));
	}
	
	@Test
	void testPartMatch() {
		HostMatcher matcher = new HostMatcher(
				Collections.emptySet(),
				Set.of("example.com"),
				Collections.emptySet()
		);
		assertTrue(matcher.matches("host.example.com"));
		assertTrue(matcher.matches(".example.com"));
		assertFalse(matcher.matches("host.github.com"));
		assertFalse(matcher.matches("example.com"));
		assertFalse(matcher.matches("example.com."));
		assertFalse(matcher.matches(""));
		assertFalse(matcher.matches("."));
	}
	
	@Test
	void testRegexMatch() {
		HostMatcher hostMatcher = new HostMatcher(
				Collections.emptySet(),
				Collections.emptySet(),
				Set.of(Pattern.compile("ex.+\\.com"))
		);
		
		assertTrue(hostMatcher.matches("example.com"));
		assertFalse(hostMatcher.matches("ex.com"));
		assertFalse(hostMatcher.matches("github.com"));
		assertFalse(hostMatcher.matches(""));
	}
}
