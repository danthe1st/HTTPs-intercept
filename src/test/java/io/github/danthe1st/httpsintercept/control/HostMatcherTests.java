package io.github.danthe1st.httpsintercept.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class HostMatcherTests {
	@Test
	void testExactMatch() {
		HostMatcher matcher = new HostMatcher(
				Set.of("example.com"),
				Collections.emptySet(),
				Collections.emptyList()
		);
		assertTrue(matcher.matches("example.com"));
		assertFalse(matcher.matches("github.com"));
	}
	
	@Test
	void testPartMatch() {
		HostMatcher matcher = new HostMatcher(
				Collections.emptySet(),
				Set.of("example.com"),
				Collections.emptyList()
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
				List.of(Pattern.compile("ex.+\\.com"))
		);
		
		assertTrue(hostMatcher.matches("example.com"));
		assertFalse(hostMatcher.matches("ex.com"));
		assertFalse(hostMatcher.matches("github.com"));
		assertFalse(hostMatcher.matches(""));
	}
	
	@Test
	void testLoadExact() {
		HostMatcher hostMatcher = HostMatcher.load(List.of("example.com"));
		assertEquals(Set.of("example.com"), hostMatcher.getExactHosts());
		assertEquals(Collections.emptySet(), hostMatcher.getHostParts());
	}
	
	@Test
	void testLoadParts() {
		HostMatcher hostMatcher = HostMatcher.load(List.of("*.example.com"));
		assertEquals(Collections.emptySet(), hostMatcher.getExactHosts());
		assertEquals(Set.of("example.com"), hostMatcher.getHostParts());
		
		assertTrue(hostMatcher.matches("host.example.com"));
	}
	
	@Test
	void testLoadEmptyPart() {
		HostMatcher hostMatcher = HostMatcher.load(List.of("*."));
		assertEquals(Collections.emptySet(), hostMatcher.getExactHosts());
		assertEquals(Set.of(""), hostMatcher.getHostParts());
		
		assertFalse(hostMatcher.matches("something"));
		assertFalse(hostMatcher.matches("example.com"));
	}
	
	@Test
	void testLoadRegex() {
		HostMatcher hostMatcher = HostMatcher.load(List.of("/ex.+\\.com"));
		assertTrue(hostMatcher.matches("example.com"));
		assertFalse(hostMatcher.matches("github.com"));
		
		hostMatcher = HostMatcher.load(List.of("/.*"));
		assertTrue(hostMatcher.matches("example.com"));
		assertTrue(hostMatcher.matches("github.com"));
		assertTrue(hostMatcher.matches("localhost"));
		assertTrue(hostMatcher.matches(""));
	}
}
