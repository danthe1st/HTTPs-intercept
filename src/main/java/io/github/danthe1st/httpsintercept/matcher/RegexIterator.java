package io.github.danthe1st.httpsintercept.matcher;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

final class RegexIterator<T> extends IteratingIterator<T> {
	private final String hostname;
	private Iterator<Entry<Pattern, List<T>>> regexes;
	private Iterator<T> current = Collections.emptyIterator();
	
	RegexIterator(Map<Pattern, List<T>> hostRegexes, String hostname) {
		this.hostname = hostname;
		this.regexes = hostRegexes.entrySet().iterator();
	}
	
	@Override
	protected Iterator<T> findNextIterator() {
		while(!current.hasNext()){
			if(!regexes.hasNext()){
				return current;// empty
			}
			Entry<Pattern, List<T>> entry = regexes.next();
			if(!entry.getKey().matcher(hostname).matches()){
				return Collections.emptyIterator();
			}
			current = entry.getValue().iterator();
		}
		return current;
	}
}