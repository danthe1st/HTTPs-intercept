package io.github.danthe1st.httpsintercept.rules.post;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import io.github.danthe1st.httpsintercept.config.HostMatcherConfig;
import io.github.danthe1st.httpsintercept.handler.http.HttpResponseContentAccessor;
import io.github.danthe1st.httpsintercept.rules.PostForwardRule;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public record HtmlBasedBlocker(
		HostMatcherConfig hostMatcher,
		String selector, Pattern matcher,
		int status, String responseContentType, String responsePath)
		implements PostForwardRule {
	
	public HtmlBasedBlocker(HostMatcherConfig hostMatcher,
			String selector, Pattern matcher,
			int status, String responseContentType,
			String responsePath) {
		if(status <= 0){
			status = 500;
		}
		if(responseContentType==null) {
			responseContentType = "text/html";
		}
		
		this.hostMatcher = hostMatcher;
		this.selector = selector;
		this.matcher = matcher;
		this.status = status;
		this.responseContentType = responseContentType;
		this.responsePath = responsePath;
	}
	
	@Override
	public boolean processRequest(FullHttpRequest fullHttpRequest, FullHttpResponse response, HttpResponseContentAccessor responseContentAccessor, Channel channel) {
		String contentType = response.headers().get("Content-Type");
		if(contentType.startsWith("text/html")){
			String html = responseContentAccessor.getAsString();
			Document doc = Jsoup.parse(html);
			for(Element element : doc.select(selector)){
				if(matcher.matcher(element.html()).matches()){
					FullHttpResponse newResponse;
					try{
						newResponse = new DefaultFullHttpResponse(
								HttpVersion.HTTP_1_1,
								HttpResponseStatus.valueOf(status),
								Unpooled.copiedBuffer(getResponseBytes())
						);
						newResponse.headers().add("Content-Type", contentType);
					}catch(IOException e){
						throw new UncheckedIOException(e);
					}
					channel.writeAndFlush(newResponse);
					return false;
				}
			}
		}
		
		return true;
	}
	
	private byte[] getResponseBytes() throws IOException {
		if(responsePath == null){
			return """
					<!DOCTYPE html>
					<html>
						<head>
							<title>Blocked</title>
						</head>
						<body>
							<h1>Content has been blocked!</h1>
							<p>
								<a href="https://github.com/danthe1st/HTTPs-intercept/">HTTPs-intercept</a> blocked this response.
							</p>
						</body>
					</html>
					""".getBytes();
		}
		return Files.readAllBytes(Path.of(responsePath));
	}
	
}
