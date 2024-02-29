package io.github.danthe1st.httpsintercept;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import io.github.danthe1st.httpsintercept.handler.ServerHandlersInit;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HttpsIntercept {
	private static final int LOCAL_PORT = Integer.getInteger("localPort", 1337);
	
	public static void main(String[] args) throws InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
		EventLoopGroup bossGroup = new NioEventLoopGroup(r -> Thread.ofVirtual().unstarted(r));// NOSONAR using shutdownGracefully instead of close
		EventLoopGroup workerGroup = new NioEventLoopGroup(r -> Thread.ofVirtual().unstarted(r));// NOSONAR using shutdownGracefully instead of close
		
		try{// NOSONAR using shutdownGracefully instead of close
			Bootstrap clientBootstrap = new Bootstrap()
				.group(workerGroup)
				.channel(NioSocketChannel.class);
			
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ServerHandlersInit(clientBootstrap))
				.childOption(ChannelOption.AUTO_READ, true)
				.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
