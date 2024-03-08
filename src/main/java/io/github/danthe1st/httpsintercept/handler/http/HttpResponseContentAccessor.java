package io.github.danthe1st.httpsintercept.handler.http;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import org.bouncycastle.util.Arrays;

public class HttpResponseContentAccessor {
	private ByteBuf contentBuf;
	private byte[] bytes;
	
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

	private void ensureBytesPresent() {
		if(bytes == null){
			ByteBuf buf = contentBuf.copy();
			bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
		}
	}
}