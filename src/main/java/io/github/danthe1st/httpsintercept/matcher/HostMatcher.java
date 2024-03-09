package io.github.danthe1st.httpsintercept.matcher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;

public final class HostMatcher<T> {
	private final Map<String, List<T>> exactHosts;
	private final Map<String, List<T>> hostParts;
	private final Map<Pattern, List<T>> hostRegexes;
	private final List<T> wildcards;
	
	public HostMatcher(List<Map.Entry<HostMatcherConfig, T>> configs, boolean allowWildcard) {
		Map<String, List<T>> hosts = new HashMap<>();
		Map<String, List<T>> parts = new HashMap<>();
		Map<Pattern, List<T>> regexes = new HashMap<>();
		List<T> wildcardElements = new ArrayList<>();
		for(Map.Entry<HostMatcherConfig, T> entry : configs){
			HostMatcherConfig config = entry.getKey();
			T value = entry.getValue();
			if(allowWildcard && config.exact().isEmpty() && config.partial().isEmpty() && config.regex().isEmpty()){
				wildcardElements.add(value);
			}else{
				addToMap(hosts, value, config.exact(), Function.identity());
				addToMap(parts, value, config.partial(), Function.identity());
				addToMap(regexes, value, config.regex(), Pattern::compile);
			}
		}
		this.exactHosts = toImmutable(hosts);
		this.hostParts = toImmutable(parts);
		this.hostRegexes = toImmutable(regexes);
		this.wildcards = List.copyOf(wildcardElements);
	}
	
	private <K> void addToMap(Map<K, List<T>> multimap, T value, Set<String> configValue, Function<String, K> keyTransformer) {
		for(String host : configValue){
			multimap
				.computeIfAbsent(keyTransformer.apply(host), h -> new ArrayList<>())
				.add(value);
		}
	}
	
	private <K> Map<K, List<T>> toImmutable(Map<K, List<T>> multimap) {
		multimap.replaceAll((k, list) -> List.copyOf(list));
		return Map.copyOf(multimap);
	}
	
	public Iterator<T> allMatches(String hostname) {
		Queue<Iterator<T>> iterators = new ArrayDeque<>();
		
		if(exactHosts.containsKey(hostname)){
			iterators.add(exactHosts.get(hostname).iterator());
		}
		if(!hostParts.isEmpty()){
			iterators.add(new HostPartIterator<>(hostname, hostParts));
		}
		iterators.add(new RegexIterator<>(hostRegexes, hostname));
		iterators.add(wildcards.iterator());
		
		Iterator<T> it = new IteratingIterator<>() {
			private Iterator<T> current = iterators.poll();
			
			@Override
			protected Iterator<T> findNextIterator() {
				while(current != null && !current.hasNext()){
					current = iterators.poll();
				}
				if(current == null){
					return Collections.emptyIterator();
				}
				return current;
			}
		};
		
		return distinctIterator(it);
	}

	private Iterator<T> distinctIterator(Iterator<T> it) {
		Set<T> matchers = Collections.newSetFromMap(new IdentityHashMap<>());
		return new FilterIterator<>(it, element -> {
			if(!matchers.contains(element)){
				matchers.add(element);
				return true;
			}
			return false;
		});
	}
	
	public Iterable<T> matchesAsIterable(String hostname) {
		return () -> allMatches(hostname);
	}
}
