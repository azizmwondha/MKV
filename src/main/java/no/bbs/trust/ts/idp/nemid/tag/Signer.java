package no.bbs.trust.ts.idp.nemid.tag;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.utils.EventLogger;


public class Signer {
	protected static Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	private final String keystorePath;
	
	private final String keyAlias;
	private final String keyPwd;
	private final String certificate;
	private final KeyStore keyStore;

	public Signer(String keystorePath, String keyAlias, String keyPwd,KeyStore keystore) {
		this.keystorePath = keystorePath;
		this.keyAlias = keyAlias;
		this.keyPwd = keyPwd;
		this.keyStore = keystore;
		this.certificate = loadCertificate(keyAlias);
	}

	public String getCertificate() {
		return certificate;
	}

	private String loadCertificate(String keyAlias) {
		try {
			Certificate certificateFromKeyStore = keyStore.getCertificate(keyAlias);
			byte[] encodedCertificate = certificateFromKeyStore.getEncoded();
			String base64EncodedCertificate = new String(Base64.encode(encodedCertificate));
			return base64EncodedCertificate.replace("\r", "").replace("\n", "");
		} catch (Exception e) {
			logger.warn("Failed to load certificate from keystore [keystorePath=" + keystorePath + "][keyAlias=" + keyAlias + "]");
			EventLogger.dumpStack(e, Level.INFO);
			throw new RuntimeException(e);
		}
	}

	public byte[] calculateSignature(byte[] data) {
		try {
			PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, keyPwd.toCharArray());
			Signature signer = Signature.getInstance("SHA256withRSA");

			signer.initSign(key);
			signer.update(data);

			return signer.sign();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	}
