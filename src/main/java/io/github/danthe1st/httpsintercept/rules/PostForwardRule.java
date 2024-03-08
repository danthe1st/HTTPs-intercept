package io.github.danthe1st.httpsintercept.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.danthe1st.httpsintercept.handler.http.HttpResponseContentAccessor;
import io.github.danthe1st.httpsintercept.rules.post.HtmlBasedBlocker;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(name = "htmlBasedBlock", value = HtmlBasedBlocker.class) })
public interface PostForwardRule extends ProcessingRule {
	boolean processRequest(FullHttpRequest fullHttpRequest, FullHttpResponse response, HttpResponseContentAccessor responseContentAccessor, Channel channel);
}
