package me.jasonbaik.loadtester.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLUtil {

	public static SSLContext createSSLContext(String keyStoreFile, String keyStorePassword, String trustStoreFile, String trustStorePassword) throws NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, IOException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new FileInputStream(new File(keyStoreFile)), keyStorePassword.toCharArray());
		KeyManagerFactory keyManagerfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerfactory.init(keyStore, keyStorePassword.toCharArray());

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(new FileInputStream(new File(trustStoreFile)), trustStorePassword.toCharArray());
		TrustManagerFactory trustManagerfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerfactory.init(trustStore);

		sslContext.init(keyManagerfactory.getKeyManagers(), trustManagerfactory.getTrustManagers(), null);
		return sslContext;
	}

}
