package no.bbs.trust.ts.idp.nemid.tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.utils.EventLogger;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;


public class Signer
{
	protected static Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	private final String keystorePath;
	private final String keystorePwd;
	private final String keyAlias;
	private final String keyPwd;
	private final String certificate;
	private final KeyStore keyStore;

	public Signer(String keystorePath, String keystorePwd, String keyAlias, String keyPwd)
	{
		this.keystorePath = keystorePath;
		this.keystorePwd = keystorePwd;
		this.keyAlias = keyAlias;
		this.keyPwd = keyPwd;
		this.keyStore = loadKeyStore();
		this.certificate = loadCertificate(keyAlias);
	}

	public String getCertificate()
	{
		return certificate;
	}

	private String loadCertificate(String keyAlias)
	{
		try
		{
			Certificate certificateFromKeyStore = keyStore.getCertificate(keyAlias);
			byte[] encodedCertificate = certificateFromKeyStore.getEncoded();
			String base64EncodedCertificate =new String(Base64.encode(encodedCertificate));
			return base64EncodedCertificate.replace("\r", "").replace("\n", "");
		}
		catch (Exception e)
		{
			logger.warn("Failed to load certificate from keystore [keystorePath=" + keystorePath + "][keyAlias=" + keyAlias + "]");
			EventLogger.dumpStack(e, Level.INFO);
			throw new RuntimeException(e);
		}
	}

	public byte[] calculateSignature(byte[] data)
	{
		try
		{
			PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, keyPwd.toCharArray());
			Signature signer = Signature.getInstance("SHA256withRSA");

			signer.initSign(key);
			signer.update(data);

			return signer.sign();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore loadKeyStore()
	{
		InputStream ksStream = null;
		try
		{
			String storeType = (keystorePath.toLowerCase().endsWith(".p12")) ? "PKCS12" : "JKS";

			KeyStore keyStore = KeyStore.getInstance(storeType);
			File keystoreFile = new File(keystorePath);
			if (keystoreFile == null || !keystoreFile.exists())
			{
				throw new RuntimeException("could not find keystore: " + keystoreFile.getCanonicalPath());
			}

			ksStream = new FileInputStream(keystoreFile);
			keyStore.load(ksStream, keystorePwd.toCharArray());
			return keyStore;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
