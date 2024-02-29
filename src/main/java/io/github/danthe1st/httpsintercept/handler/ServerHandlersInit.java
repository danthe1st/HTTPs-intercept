package io.github.danthe1st.httpsintercept.handler;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import io.github.danthe1st.httpsintercept.handler.http.IncomingHttpRequestHandler;
import io.github.danthe1st.httpsintercept.handler.sni.CustomSniHandler;
import io.github.danthe1st.httpsintercept.handler.sni.SNIHandlerMapping;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandlersInit extends ChannelInitializer<SocketChannel> {
	
	static final Logger LOG = LoggerFactory.getLogger(ServerHandlersInit.class);
	
	private final Bootstrap clientBootstrapTemplate;
	private final SslContext clientSslContext;
	private final SNIHandlerMapping sniMapping;
	
	public ServerHandlersInit(Bootstrap clientBootstrap) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		this.clientBootstrapTemplate = clientBootstrap;
		sniMapping = SNIHandlerMapping.createMapping();
		
		clientSslContext = SslContextBuilder.forClient().build();
	}
	
	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		SniHandler sniHandler = new CustomSniHandler(sniMapping, clientBootstrapTemplate);
		socketChannel.pipeline().addLast(
				sniHandler,
				new HttpServerCodec(),
				new HttpObjectAggregator(1048576),
				new IncomingHttpRequestHandler(sniHandler, clientBootstrapTemplate)
		);
	}
}
