package io.github.danthe1st.httpsintercept.matcher;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

final class FilterIterator<T> implements Iterator<T> {
	private final Iterator<T> iterator;
	private final Predicate<T> filter;
	private T current;
	
	public FilterIterator(Iterator<T> iterator, Predicate<T> filter) {
		this.iterator = iterator;
		this.filter = filter;
	}
	@Override
	public boolean hasNext() {
		if(current != null){
			return true;
		}
		while(iterator.hasNext()){
			T next = iterator.next();
			if(filter.test(next)){
				current = next;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public T next() {
		if(!hasNext()){
			throw new NoSuchElementException();
		}
		T ret = current;
		current = null;
		return ret;
	}
	
}
