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
import org.checkerframework.checker.nullness.qual.NonNull;

public final class HostMatcher<T> {
	private final Map<String, List<@NonNull T>> exactHosts;
	private final Map<String, List<@NonNull T>> hostParts;
	private final Map<Pattern, List<@NonNull T>> hostRegexes;
	private final List<@NonNull T> wildcards;
	
	public HostMatcher(List<Map.Entry<HostMatcherConfig, @NonNull T>> configs, boolean allowWildcard) {
		Map<String, List<@NonNull T>> hosts = new HashMap<>();
		Map<String, List<@NonNull T>> parts = new HashMap<>();
		Map<Pattern, List<@NonNull T>> regexes = new HashMap<>();
		List<@NonNull T> wildcardElements = new ArrayList<>();
		for(Map.Entry<HostMatcherConfig, @NonNull T> entry : configs){
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
	
	private static <T, K> void addToMap(Map<K, List<@NonNull T>> multimap, @NonNull T value, Set<String> configValue, Function<String, K> keyTransformer) {
		for(String host : configValue){
			List<@NonNull T> list = multimap.merge(
					keyTransformer.apply(host),
					new ArrayList<>(),
					(existing, emptyList) -> existing
			);
			if(list == null){
				throw new NullPointerException();
			}
			list.add(value);
		}
	}
	
	private static <T, K> Map<@NonNull K, List<@NonNull T>> toImmutable(Map<@NonNull K, List<@NonNull T>> multimap) {
		multimap.replaceAll((k, list) -> List.copyOf(list));
		return Map.copyOf(multimap);
	}
	
	public Iterator<@NonNull T> allMatches(String hostname) {
		Queue<Iterator<@NonNull T>> iterators = new ArrayDeque<>();
		
		List<@NonNull T> exactHostElements = exactHosts.get(hostname);
		if(exactHostElements != null){
			iterators.add(exactHostElements.iterator());
		}
		if(!hostParts.isEmpty()){
			iterators.add(new HostPartIterator<>(hostname, hostParts));
		}
		iterators.add(new RegexIterator<>(hostRegexes, hostname));
		iterators.add(wildcards.iterator());
		
		return distinctIterator(new ConcatenatingIterator<>(iterators));
	}
	
	private Iterator<@NonNull T> distinctIterator(Iterator<@NonNull T> it) {
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
