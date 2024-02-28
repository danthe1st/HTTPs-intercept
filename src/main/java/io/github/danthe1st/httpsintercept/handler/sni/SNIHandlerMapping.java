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
import java.util.List;

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
	
	
	private final KeyStore ks;
	private final char[] privateKeyPassword;
	
	public SNIHandlerMapping() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		
		Path secretFile = Path.of(".secret");
		if(!Files.exists(secretFile)){
			throw new IllegalStateException("Cannot find .secret file - Make sure it was generated using the certs.sh script");
		}
		List<String> secretLines = Files.readAllLines(secretFile);
		if(secretLines.size() != 2){
			throw new IllegalStateException("Cannot parse .secret file - Make sure it was generated using the certs.sh script");
		}
		
		char[] passphrase = secretLines.get(0).toCharArray();
		privateKeyPassword = secretLines.get(1).toCharArray();
		
		LOG.info("Initiating SSL context");
		
		ks = KeyStore.getInstance("JKS");
		
		try(InputStream is = Files.newInputStream(Path.of(KEYSTORE))){
			ks.load(is, passphrase);
		}
	}

	private KeyPair extractKeyPair(KeyStore ks, String keyName, char[] passphrase) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		return new KeyPair(ks.getCertificate(keyName).getPublicKey(), (PrivateKey)ks.getKey(keyName, passphrase));
	}
	
	@Override
	public SslContext map(String hostname) {
		LOG.debug("loadding certificate for hostname {}", hostname);
		try{
			if(hostname==null) {
				LOG.warn("using default key manager because hostname is missing");
				hostname="localhost";
			}
			// TODO cache certs of hostnames
			char[] passphrase = privateKeyPassword;
			KeyPair serverKeyPair = extractKeyPair(ks, "server", passphrase);
			KeyPair rootKeyPair = extractKeyPair(ks, "root", passphrase);
			X509Certificate rootCert = (X509Certificate) ks.getCertificate("root");
			X509Certificate newCert = CertificateGenerator.createCertificate(serverKeyPair, hostname, rootKeyPair, rootCert, false);
			
			return SslContextBuilder.forServer(serverKeyPair.getPrivate(), (String)null, newCert, rootCert).build();
		}catch(SSLException | KeyStoreException | CertIOException | OperatorCreationException | CertificateException | UnrecoverableKeyException | NoSuchAlgorithmException e){
			throw new CertificateGenerationException("ailed to initialize the server-side SSLContext", e);
		}
	}
	
	private static class CertificateGenerationException extends RuntimeException {
		
		public CertificateGenerationException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
	
}
