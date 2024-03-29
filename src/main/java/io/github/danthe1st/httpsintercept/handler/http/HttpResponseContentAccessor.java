package io.github.danthe1st.httpsintercept.handler.http;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import org.bouncycastle.util.Arrays;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HttpResponseContentAccessor {
	private ByteBuf contentBuf;
	private byte @Nullable [] bytes;
	
	public HttpResponseContentAccessor(FullHttpResponse res) {
		contentBuf = res.content();
	}
	
	public byte[] getBytes() {
		ensureBytesPresent();
		return Arrays.copyOf(bytes, bytes.length);
	}
	
	public String getAsString() {
		ensureBytesPresent();
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	@EnsuresNonNull("bytes")
	private void ensureBytesPresent() {
		if(bytes == null){
			byte[] b;
			ByteBuf buf = contentBuf.copy();
			try{
				b = new byte[buf.readableBytes()];
				buf.readBytes(b);
			}finally{
				buf.release();
			}
			bytes = b;
		}
	}
}