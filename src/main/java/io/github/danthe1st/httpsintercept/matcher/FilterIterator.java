package io.github.danthe1st.httpsintercept.matcher;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class FilterIterator<@NonNull T> implements Iterator<T> {
	private final Iterator<@NonNull T> iterator;
	private final Predicate<@NonNull T> filter;
	private @Nullable T current;
	
	public FilterIterator(Iterator<@NonNull T> iterator, Predicate<@NonNull T> filter) {
		this.iterator = iterator;
		this.filter = filter;
	}
	
	@Override
	@EnsuresNonNullIf(expression = "current", result = true)
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
