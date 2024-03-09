package io.github.danthe1st.httpsintercept.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import org.junit.jupiter.api.Test;

class HostMatcherTests {
	
	@Test
	void testExactMatch() {
		HostMatcher<Object> matcher = createMatcher(
				Set.of("example.com"),
				Collections.emptySet(),
				Collections.emptySet()
		);
		expectSingleMatch(matcher, "example.com");
		expectNoMatches(matcher, "github.com");
	}
	
	@Test
	void testDoubleExactMatchDifferentObjects() {
		HostMatcher<Object> matcher = new HostMatcher<>(
				List.of(
						Map.entry(new HostMatcherConfig(Set.of("example.com"), null, null), 1),
						Map.entry(new HostMatcherConfig(Set.of("example.com"), null, null), 2)
				), false
		);
		expectMatches(matcher, "example.com", 1, 2);
	}
	
	@Test
	void testEmptyPartMatch() {
		HostMatcher<Object> matcher = createMatcher(
				Collections.emptySet(),
				Set.of(""),
				Collections.emptySet()
		);
		expectNoMatches(matcher, "localhost");
	}
	
	@Test
	void testPartMatch() {
		HostMatcher<Object> matcher = createMatcher(
				Collections.emptySet(),
				Set.of("example.com"),
				Collections.emptySet()
		);
		expectSingleMatch(matcher, "host.example.com");
		expectSingleMatch(matcher, ".example.com");
		expectNoMatches(matcher, "host.github.com");
		expectNoMatches(matcher, "github.com");
		expectSingleMatch(matcher, "example.com");
		expectNoMatches(matcher, "example.com.");
		expectNoMatches(matcher, "");
		expectNoMatches(matcher, ".");
	}
	
	@Test
	void testDoublePartMatchDifferentObjects() {
		HostMatcher<Object> matcher = new HostMatcher<>(
				List.of(
						Map.entry(new HostMatcherConfig(null, Set.of("example.com"), null), 1),
						Map.entry(new HostMatcherConfig(null, Set.of("com"), null), 2)
				), false
		);
		expectMatches(matcher, "example.com", 1, 2);
		expectMatches(matcher, "test.com", 2);
	}
	
	@Test
	void testRegexMatch() {
		HostMatcher<Object> hostMatcher = createMatcher(
				Collections.emptySet(),
				Collections.emptySet(),
				Set.of("ex.+\\.com")
		);
		
		expectSingleMatch(hostMatcher, "example.com");
		expectNoMatches(hostMatcher, "ex.com");
		expectNoMatches(hostMatcher, "github.com");
		expectNoMatches(hostMatcher, "");
	}
	
	@Test
	void testMultiRegexMatchDifferentObjects() {
		HostMatcher<Object> matcher = new HostMatcher<>(
				List.of(
						Map.entry(new HostMatcherConfig(null, null, Set.of("example\\.com")), 1),
						Map.entry(new HostMatcherConfig(null, null, Set.of("ex.*")), 2),
						Map.entry(new HostMatcherConfig(null, null, Set.of(".*")), 3)
				), false
		);
		expectMatches(matcher, "example.com", 1, 2, 3);
		expectMatches(matcher, "ex123456.com", 2, 3);
		expectMatches(matcher, "test.com", 3);
	}
	
	@Test
	void testMultiMatchSameObjectDifferentCategories() {
		HostMatcher<Object> matcher = createMatcher(
				Set.of("example.com"),
				Set.of("example.com", "com"),
				Set.of("e.*", ".*")
		);
		expectSingleMatch(matcher, "example.com");
	}
	
	@Test
	void testNoMatchWithoutConfig() {
		HostMatcher<Object> matcher = new HostMatcher<>(List.of(), false);
		expectNoMatches(matcher, "");
		expectNoMatches(matcher, "example.com");
	}
	
	// TODO wildcard
	
	private HostMatcher<Object> createMatcher(Set<String> exactHosts, Set<String> hostParts, Set<String> hostRegexes) {
		return new HostMatcher<>(List.of(Map.entry(new HostMatcherConfig(exactHosts, hostParts, hostRegexes), 1)), false);
	}
	
	private void expectSingleMatch(HostMatcher<Object> hostMatcher, String host) {
		expectMatches(hostMatcher, host, 1);
	}
	
	private void expectNoMatches(HostMatcher<Object> hostMatcher, String host) {
		expectMatches(hostMatcher, host);
	}
	
	private void expectMatches(HostMatcher<Object> hostMatcher, String host, Object... expectedMatches) {
		Iterator<Object> allMatches = hostMatcher.allMatches(host);
		
		Set<Object> furtherExpectedMatches = new HashSet<>(Set.of(expectedMatches));
		while(!furtherExpectedMatches.isEmpty()){
			assertTrue(allMatches.hasNext(), "Missing matches: " + furtherExpectedMatches);
			Object next = allMatches.next();
			assertTrue(furtherExpectedMatches.contains(next), "Unexpected match: " + next);
			furtherExpectedMatches.remove(next);
		}
		assertFalse(allMatches.hasNext(), "Unexpected match after all expected matches found");
		assertThrows(NoSuchElementException.class, allMatches::next);
	}
}
