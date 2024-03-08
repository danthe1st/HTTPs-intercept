package io.github.danthe1st.httpsintercept.matcher;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class HostPartIterator<T> extends IteratingIterator<T> {
	private final String hostname;
	private int index = 0;
	private Iterator<T> current = Collections.emptyIterator();
	private final Map<String, List<T>> hostParts;
	
	HostPartIterator(String hostname, Map<String, List<T>> hostParts) {
		this.hostname = hostname;
		this.hostParts = hostParts;
	}
	
	@Override
	protected Iterator<T> findNextIterator() {
		if(current.hasNext()){
			return current;
		}
		do{
			String hostPart = hostname.substring(index);
			if(hostParts.containsKey(hostPart)){
				current = hostParts.get(hostPart).iterator();
				if(current.hasNext()){
					nextIndex();
					return current;
				}
			}
		}while(nextIndex() != 0 && index < hostname.length());
		return Collections.emptyIterator();
	}

	private int nextIndex() {
		return index = hostname.indexOf('.', index) + 1;
	}
}