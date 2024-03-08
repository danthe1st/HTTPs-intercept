package io.github.danthe1st.httpsintercept.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import org.junit.jupiter.api.Test;

class HostMatcherTests {
	
	private IterativeHostMatcher<Object> createMatcher(Set<String> exactHosts, Set<String> hostParts, Set<String> hostRegexes) {
		return new IterativeHostMatcher<>(List.of(Map.entry(new HostMatcherConfig(exactHosts, hostParts, hostRegexes), new Object())));
	}
	
	@Test
	void testExactMatch() {
		IterativeHostMatcher<Object> matcher = createMatcher(
				Set.of("example.com"),
				Collections.emptySet(),
				Collections.emptySet()
		);
		assertTrue(matcher.allMatches("example.com").hasNext());
		assertFalse(matcher.allMatches("github.com").hasNext());
	}
	
	@Test
	void testEmptyPartMatch() {
		IterativeHostMatcher<Object> matcher = createMatcher(
				Collections.emptySet(),
				Set.of(""),
				Collections.emptySet()
		);
		assertFalse(matcher.allMatches("localhost").hasNext());
	}
	
	@Test
	void testPartMatch() {
		IterativeHostMatcher<Object> matcher = createMatcher(
				Collections.emptySet(),
				Set.of("example.com"),
				Collections.emptySet()
		);
		assertTrue(matcher.allMatches("host.example.com").hasNext());
		assertTrue(matcher.allMatches(".example.com").hasNext());
		assertFalse(matcher.allMatches("host.github.com").hasNext());
		assertTrue(matcher.allMatches("example.com").hasNext());
		assertFalse(matcher.allMatches("example.com.").hasNext());
		assertFalse(matcher.allMatches("").hasNext());
		assertFalse(matcher.allMatches(".").hasNext());
	}
	
	@Test
	void testRegexMatch() {
		IterativeHostMatcher<Object> hostMatcher = createMatcher(
				Collections.emptySet(),
				Collections.emptySet(),
				Set.of("ex.+\\.com")
		);
		
		assertTrue(hostMatcher.allMatches("example.com").hasNext());
		assertFalse(hostMatcher.allMatches("ex.com").hasNext());
		assertFalse(hostMatcher.allMatches("github.com").hasNext());
		assertFalse(hostMatcher.allMatches("").hasNext());
	}
}
