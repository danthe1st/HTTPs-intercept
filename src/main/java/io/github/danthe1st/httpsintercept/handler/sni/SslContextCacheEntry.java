package io.github.danthe1st.httpsintercept.handler.sni;

import java.util.Objects;

import io.netty.handler.ssl.SslContext;

class SslContextCacheEntry {
	private final SslContext sslContext;
	private volatile long lastAccessTime;
	
	public SslContextCacheEntry(SslContext sslContext) {
		this.sslContext = Objects.requireNonNull(sslContext);
		refresh();
	}
	
	public SslContext getSslContext() {
		refresh();
		return sslContext;
	}
	
	private void refresh() {
		lastAccessTime = System.currentTimeMillis();
	}
	
	public long getLastAccessTime() {
		return lastAccessTime;
	}
}