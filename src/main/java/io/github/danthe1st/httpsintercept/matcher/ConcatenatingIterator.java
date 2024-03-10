package io.github.danthe1st.httpsintercept.matcher;

import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class ConcatenatingIterator<@NonNull T> extends IteratingIterator<T> {
	private final Queue<Iterator<T>> iterators;
	private @Nullable Iterator<T> current;
	
	ConcatenatingIterator(Queue<Iterator<T>> iterators) {
		this.iterators = iterators;
		current = iterators.poll();
	}
	
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
}