package io.github.danthe1st.httpsintercept.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(name = "addHeader", value = SetHeaderRule.class) })
public interface PreForwardRule extends ProcessingRule {
	boolean processRequest(FullHttpRequest fullHttpRequest, Channel channel);
}
