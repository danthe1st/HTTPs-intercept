package io.github.danthe1st.httpsintercept.matcher;

import java.util.Iterator;

abstract class IteratingIterator<T> implements Iterator<T> {
	
	protected abstract Iterator<T> findNextIterator();
	
	@Override
	public boolean hasNext() {
		return findNextIterator().hasNext();
	}
	
	@Override
	public T next() {
		return findNextIterator().next();
	}
}
