package no.bbs.trust.ts.idp.nemid.verify;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.util.encoders.Base64;
import org.openoces.ooapi.certificate.CertificateStatus;
import org.openoces.ooapi.certificate.OcesCertificate;
import org.openoces.ooapi.exceptions.AppletException;
import org.openoces.ooapi.exceptions.InternalException;
import org.openoces.ooapi.exceptions.NonOpensignSignatureException;
import org.openoces.ooapi.signatures.OpensignAbstractSignature;
import org.openoces.ooapi.signatures.OpensignSignature;
import org.openoces.ooapi.signatures.OpensignSignatureFactory;
import org.openoces.ooapi.signatures.SignatureProperty;
import org.openoces.ooapi.utils.Base64Handler;
import org.openoces.ooapi.validation.ErrorCodeChecker;
import org.openoces.securitypackage.SignatureValidationStatus;
import org.openoces.serviceprovider.ServiceProviderException;
import org.openoces.serviceprovider.ServiceProviderSetup;

public class SignHandlerHack {
	public static SignatureValidationStatus validateSignatureAgainstAgreement(String loginData, String agreement, String signTextTransformation, String challenge, String logonto)
			throws ServiceProviderException, AppletException {
		if (ErrorCodeChecker.isError(loginData))
			throw new AppletException(ErrorCodeChecker.extractError(loginData));
		try {
			
			
			OpensignSignature opensignSignature = createOpensignSignature(Base64Handler.decode(loginData));			
			validateSignatureParameters(challenge, opensignSignature, logonto);
			String encodedSignature = encodeSignature(opensignSignature);
			
			OcesCertificate certificate = opensignSignature.getSigningCertificate();
			CertificateStatus status = certificate.validityStatus();
			if ((status == CertificateStatus.VALID) && (ServiceProviderSetup.getCurrentChecker().isRevoked(certificate))) {
				status = CertificateStatus.REVOKED;
			}

			// This is the hack: usign agreement instead of encodedAgreement
			boolean signatureMatches = signatureMatches(encodedSignature, agreement, signTextTransformation, opensignSignature);
			return new SignatureValidationStatus(opensignSignature, status, signatureMatches);
		} catch (NonOpensignSignatureException e) {
			throw new ServiceProviderException(e);
		} catch (InternalException e) {
			throw new ServiceProviderException(e);
		}
	}

	public static SignatureValidationStatus validateSignatureAgainstAgreement(String loginData, String agreement, String challenge, String logonto)
			throws ServiceProviderException, AppletException {
		return validateSignatureAgainstAgreement(loginData, agreement, null, challenge, logonto);
	}

	public static SignatureValidationStatus validateSignatureAgainstAgreementPDF(String loginData, String agreement, String challenge, String logonto) throws IOException,
			ServiceProviderException, AppletException {
		if (ErrorCodeChecker.isError(loginData))
			throw new AppletException(ErrorCodeChecker.extractError(loginData));
		try {
			OpensignSignature opensignSignature = createOpensignSignature(new String(Base64.decode(loginData)));

			validateChallenge(opensignSignature, challenge);
			if (logonto != null) {
				validateLogonTo(opensignSignature, logonto);
			}

			String encodedSignature = new String(Base64.encode(opensignSignature.getSignedDocument().getSignedContent()));
			OcesCertificate certificate = opensignSignature.getSigningCertificate();
			CertificateStatus status = certificate.validityStatus();
			if ((status == CertificateStatus.VALID) && (ServiceProviderSetup.getCurrentChecker().isRevoked(certificate))) {
				status = CertificateStatus.REVOKED;
			}

			// This is the hack: usign agreement instead of encodedAgreement
			boolean signatureMatches = signatureMatches(encodedSignature, agreement, null, opensignSignature);

			return new SignatureValidationStatus(opensignSignature, status, signatureMatches);
		} catch (NonOpensignSignatureException e) {
			throw new ServiceProviderException(e);
		} catch (InternalException e) {
			throw new ServiceProviderException(e);
		}
	}

	private static boolean signatureMatches(String encodedSignature, String encodedAgreement, String signTextTransformation, OpensignSignature opensignSignature) {
		try {
			if (!(encodedAgreement.equals(encodedSignature))) {
				return false;
			}

			String stylesheetDigest = opensignSignature.getStylesheetDigest();
			if (stylesheetDigest != null) {
				if (signTextTransformation == null) {
					throw new IllegalArgumentException("signTextTransformation is required for XML signing");
				}

				MessageDigest digester = MessageDigest.getInstance("SHA256", "BC");
				String calculatedDigest = Base64Handler.encode(digester.digest(signTextTransformation.getBytes("UTF-8")));

				return stylesheetDigest.equals(calculatedDigest);
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (NoSuchProviderException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (InternalException e) {
			throw new RuntimeException(e);
		}
	}

	private static OpensignSignature createOpensignSignature(String loginData) throws NonOpensignSignatureException, InternalException {
		OpensignAbstractSignature abstractSignature = OpensignSignatureFactory.getInstance().generateOpensignSignature(loginData);
		if (!(abstractSignature instanceof OpensignSignature)) {
			throw new IllegalArgumentException("argument of type " + abstractSignature.getClass() + " is not valid output from the sign applet");
		}
		verifySignature(abstractSignature);
		return ((OpensignSignature) abstractSignature);
	}

	private static void verifySignature(OpensignAbstractSignature signature) throws InternalException {
		if (!(signature.verify()))
			throw new IllegalArgumentException("sign signature is not valid");
	}

	private static String encodeSignature(OpensignSignature opensignSignature) throws ServiceProviderException {
		try {
			byte[] signedBytes = opensignSignature.getSigntext().getBytes("UTF-8");
			return Base64Handler.encode(signedBytes);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (InternalException e) {
			throw new ServiceProviderException("Could not encode signature", e);
		}
	}

	private static void validateSignatureParameters(String challenge, OpensignSignature opensignSignature, String logonto) throws InternalException, ServiceProviderException {
		validateChallenge(opensignSignature, challenge);
		validateVisibleToSignerForSignText(opensignSignature);
		if (logonto != null)
			validateLogonTo(opensignSignature, logonto);
	}

	private static void validateChallenge(OpensignSignature opensignSignature, String challenge) throws InternalException {
		ChallengeVerifierHack.verifyChallenge(opensignSignature, challenge);
	}

	private static void validateVisibleToSignerForSignText(OpensignSignature opensignSignature) throws InternalException, ServiceProviderException {
		SignatureProperty signTextProperty = (SignatureProperty) opensignSignature.getSignatureProperties().get("signtext");
		if ((isNotSignedXmlDocument(opensignSignature)) && (!(signTextProperty.isVisibleToSigner())))
			throw new ServiceProviderException("Invalid sign signature - the parameter signtext in the signature must have the attribute visibleToSigner set to true");
	}

	private static boolean isNotSignedXmlDocument(OpensignSignature opensignSignature) throws InternalException {
		return (opensignSignature.getStylesheetDigest() == null);
	}

	private static void validateLogonTo(OpensignSignature signature, String logonto) throws ServiceProviderException, InternalException {
		SignatureProperty logontoProperty = (SignatureProperty) signature.getSignatureProperties().get("logonto");
		SignatureProperty requestIssuerProperty = (SignatureProperty) signature.getSignatureProperties().get("RequestIssuer");

		if ((logontoProperty != null) && (requestIssuerProperty != null)) {
			throw new IllegalStateException("Invalid signature logonto and RequestIssuer parameters cannot both be set");
		}

		if ((logontoProperty == null) && (requestIssuerProperty == null)) {
			throw new IllegalStateException("Invalid signature either logonto or RequestIssuer parameters must be set");
		}

		if (logontoProperty != null) {
			String logontoPropertyValue = logontoProperty.getValue();
			if (!(logontoPropertyValue.equals(logonto))) {
				throw new ServiceProviderException("Invalid signature logonto parameter does not match expected value. Expected: " + logonto + " actual: " + logontoPropertyValue);
			}

		}

		if (requestIssuerProperty != null) {
			String requestIssuerValue = requestIssuerProperty.getValue();
			if (!(requestIssuerValue.equals(logonto)))
				throw new ServiceProviderException("Invalid signature RequestIssuer parameter does not match expected value. Expected: " + logonto + " actual: "
						+ requestIssuerValue);
		}
	}
}
