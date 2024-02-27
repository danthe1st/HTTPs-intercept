package io.github.danthe1st.httpsintercept;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateGenerator {
	
	private CertificateGenerator() {
		// utility class cannot be instantiated
	}
	
	// https://gamlor.info/posts-output/2019-10-29-java-create-certs-bouncy/en/
	public static X509Certificate createCertificate(KeyPair certKeyPair, String domain, KeyPair issuerKeyPair, X509Certificate issuerCert, boolean isCA) throws CertIOException, OperatorCreationException, CertificateException {
		X500Name name = getSubject();
		// If you issue more than just test certificates, you might want a decent serial number schema ^.^
		BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
		Instant validFrom = Instant.now();
		Instant validUntil = validFrom.plus(10 * 360, ChronoUnit.DAYS);
		
		// If there is no issuer, we self-sign our certificate.
		X500Name issuerName;
		PrivateKey issuerKey;
		
		issuerName = new JcaX509CertificateHolder(((X509Certificate) issuerCert)).getSubject();
		issuerKey = issuerKeyPair.getPrivate();
		
		// The cert builder to build up our certificate information
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				issuerName,
				serialNumber,
				Date.from(validFrom), Date.from(validUntil),
				name, certKeyPair.getPublic()
		);
		
		// Make the cert to a Cert Authority to sign more certs when needed
		if(isCA){
			builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));
		}
		// Modern browsers demand the DNS name entry
		if(domain != null){
			builder.addExtension(
					Extension.subjectAlternativeName, false,
					new GeneralNames(new GeneralName(GeneralName.dNSName, domain))
			);
		}
		
		// Finally, sign the certificate:
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(issuerKey);
		X509CertificateHolder certHolder = builder.build(signer);
		return new JcaX509CertificateConverter().getCertificate(certHolder);
	}
	
	private static X500Name getSubject() {
		return new X500Name(
				new RDN[] { new RDN(
						new AttributeTypeAndValue[] {
								
								new AttributeTypeAndValue(BCStyle.CN, new DERUTF8String("interceptorCert")),
								new AttributeTypeAndValue(BCStyle.OU, new DERUTF8String("dan1st")),
								new AttributeTypeAndValue(BCStyle.O, new DERUTF8String("personal")),
								new AttributeTypeAndValue(BCStyle.C, new DERUTF8String("AT"))
						}
				) }
		);
	}
	
}