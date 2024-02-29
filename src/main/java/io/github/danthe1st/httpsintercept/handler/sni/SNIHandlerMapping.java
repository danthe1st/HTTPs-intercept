package io.github.danthe1st.httpsintercept.handler.sni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import io.github.danthe1st.httpsintercept.CertificateGenerator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.Mapping;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNIHandlerMapping implements Mapping<String, SslContext> {
	
	private static final Logger LOG = LoggerFactory.getLogger(SNIHandlerMapping.class);
	
	private static final String KEYSTORE = "interceptor.jks";
	private static final Duration CACHE_INVALIDATION_DURATION = Duration.ofSeconds(10);
	
	private final KeyStore ks;
	private final Map<String, SslContextCacheEntry> certificateCache = new ConcurrentHashMap<>();
	private final KeyPair rootKeyPair;
	private final X509Certificate rootCert;

	private final KeyPair serverKeyPair;
	
	private SNIHandlerMapping() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
		
		Path secretFile = Path.of(".secret");
		if(!Files.exists(secretFile)){
			throw new IllegalStateException("Cannot find .secret file - Make sure it was generated using the certs.sh script");
		}
		List<String> secretLines = Files.readAllLines(secretFile);
		if(secretLines.size() != 2){
			throw new IllegalStateException("Cannot parse .secret file - Make sure it was generated using the certs.sh script");
		}
		
		char[] passphrase = secretLines.get(0).toCharArray();
		char[] privateKeyPassword = secretLines.get(1).toCharArray();
		
		LOG.info("Initiating SSL context");
		
		ks = KeyStore.getInstance("JKS");
		
		try(InputStream is = Files.newInputStream(Path.of(KEYSTORE))){
			ks.load(is, passphrase);
		}
		
		rootCert = (X509Certificate) ks.getCertificate("root");
		
		rootKeyPair = new KeyPair(
				rootCert.getPublicKey(),
				(PrivateKey) ks.getKey("root", privateKeyPassword)
		);
		
		serverKeyPair = CertificateGenerator.generateKeyPair();
	}
	
	public static SNIHandlerMapping createMapping() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
		SNIHandlerMapping mapping = new SNIHandlerMapping();
		Thread.startVirtualThread(mapping::runCleanupDaemon);
		return mapping;
	}

	private void runCleanupDaemon() {
		try{
			while(true){// NOSONAR ended by potential InterruptedException (or more likely the virtual thread ending)
				long minAccessTime = System.currentTimeMillis() - CACHE_INVALIDATION_DURATION.toMillis();
				certificateCache
					.entrySet()
					.removeIf(e -> e.getValue().getLastAccessTime() < minAccessTime);
				Thread.sleep(CACHE_INVALIDATION_DURATION.toMillis());
			}
		}catch(InterruptedException e){
			Thread.currentThread().interrupt();
		}
	}

	private KeyPair extractKeyPair(KeyStore ks, String keyName, char[] passphrase) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		return new KeyPair(ks.getCertificate(keyName).getPublicKey(), (PrivateKey)ks.getKey(keyName, passphrase));
	}
	
	@Override
	public SslContext map(String hostname) {
		LOG.debug("loadding certificate for hostname {}", hostname);
		if(hostname == null){
			LOG.warn("using default key manager because hostname is missing");
			hostname = "localhost";
		}
		return certificateCache.computeIfAbsent(
				hostname,
				h -> new SslContextCacheEntry(createSslContext(h))
		).getSslContext();
	}
	
	private SslContext createSslContext(String hostname) {
		try{
			X509Certificate newCert = CertificateGenerator.createCertificate(serverKeyPair, hostname, rootKeyPair, rootCert, false);
			return SslContextBuilder.forServer(serverKeyPair.getPrivate(), (String)null, newCert, rootCert).build();
		}catch(SSLException | CertIOException | OperatorCreationException | CertificateException e){
			throw new CertificateGenerationException("ailed to initialize the server-side SSLContext", e);
		}
	}
}
