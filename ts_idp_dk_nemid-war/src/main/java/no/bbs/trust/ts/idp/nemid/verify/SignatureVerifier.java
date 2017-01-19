package no.bbs.trust.ts.idp.nemid.verify;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import no.bbs.trust.common.basics.events.ActionEvent;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.Base64;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.error.NemIDErrorMapper;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.tt.bc.cryptlib.ocsp.CertificateStatusException;
import no.bbs.tt.bc.cryptlib.ocsp.OCSPData;
import no.bbs.tt.bc.cryptlib.ocsp.OCSPRequestor;
import no.bbs.tt.bc.cryptlib.x509.X509Parser;

import org.apache.log4j.Logger;
import org.openoces.ooapi.certificate.CertificateStatus;
import org.openoces.ooapi.certificate.FocesCertificate;
import org.openoces.ooapi.certificate.MocesCertificate;
import org.openoces.ooapi.certificate.PocesCertificate;
import org.openoces.ooapi.exceptions.AppletException;
import org.openoces.securitypackage.SignatureValidationStatus;
import org.openoces.ooapi.signatures.Oces2ErrorCode;

public class SignatureVerifier {

	protected static Logger logger = Logger.getLogger("mainLogger");
	private static String CHARSET_ENCODING = "UTF-8";

	private VerifyClientSignatureData data;
	private String statusCode;
	private boolean validatedOK = false;

	public SignatureVerifier(VerifyClientSignatureData data) {
		this.data = data;
	}

	public VerifyClientSignatureResponseDataExt handleVerifyClientSignatureData() throws StatusCodeException {
		VerifyClientSignatureResponseDataExt response = new VerifyClientSignatureResponseDataExt();

		String b64signature = data.getSignature();
		logger.trace("XML signature: " + b64signature);

		String challenge = data.getChallenge();

		try {
			SignatureValidationStatus status = null;

			logger.info("Validate Signature Against Agreement for document type: " + data.getDocumentType());
			if ("application/pdf".equalsIgnoreCase(data.getDocumentType())) {
				status = SignHandlerHack.validateSignatureAgainstAgreementPDF(b64signature, data.getB64Document(), challenge, null);
			} else {
				status = SignHandlerHack.validateSignatureAgainstAgreement(b64signature, data.getB64Document(), challenge, null);
			}

			boolean isFoces = (status.getCertificate() instanceof FocesCertificate);
			boolean isMoces = (status.getCertificate() instanceof MocesCertificate);
			boolean isPoces = (status.getCertificate() instanceof PocesCertificate);

			CertificateStatus certificateStatus = status.getCertificateStatus();
			logger.info("CertificateStatus: " + certificateStatus);
			logger.info("CertificateType: " + (isFoces ? "F" : (isMoces ? "M" : (isPoces ? "P" : "Unknown-"))) + "OCES");
			logger.debug("Signature Matches: " + status.signatureMatches());

			if (certificateStatus != CertificateStatus.VALID) {
				invalidCertificate(certificateStatus);
			}

			if (isPoces) {
				PocesCertificate poces = (PocesCertificate) status.getCertificate();
				if (status.signatureMatches()) {
					X509Certificate signerCertificate = status.getCertificate().exportCertificate();
					X509Parser x509Parser = new X509Parser(signerCertificate);
					String[] ocspResponders = x509Parser.getOCSPResponderURIs();
					String ocsp = getOCSPResponse(null, null, signerCertificate, ocspResponders[0]);

					logger.debug("OCSP Response: " + ocsp);
					logger.debug("PID: " + poces.getPid());

					response.setB64ocsp(ocsp);
					response.setSignerPID(status.getCertificate().getSubjectSerialNumber());
					logger.debug("SubjectSerialNo: " + poces.getSubjectSerialNumber());

					validatedOK = true;
				} else {
					throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "The signed document is not equal to the expected document");
				}
			} else if (isMoces) {
				logger.debug("Moces certificate validation");
				MocesCertificate moces = (MocesCertificate) status.getCertificate();
				moces.getSubjectSerialNumber();
				logger.debug("SubjectSerialNo: " + moces.getSubjectSerialNumber());
				if (status.signatureMatches()) {
					X509Certificate signerCertificate = moces.exportCertificate();
					String rid = moces.getRid();
					X509Parser x509Parser = new X509Parser(signerCertificate);
					String[] ocspResponders = x509Parser.getOCSPResponderURIs();
					String ocsp = getOCSPResponse(null, null, signerCertificate, ocspResponders[0]);

					logger.debug("OCSP Response: " + ocsp);
					logger.debug("RID: " + rid);

					response.setB64ocsp(ocsp);
					response.setSignerRID(moces.getSubjectSerialNumber());

					validatedOK = true;
				} else {
					throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "The signed document is not equal to the expected document");
				}
			} else {
				throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CERT_TYPE_FAILED, "Expected MOSES or POCES but signature cert was "
						+ status.getCertificate().getClass().getName());
			}
			response.certificate = Base64.encode(status.getCertificate().getBytes(), false);
		} catch (AppletException ae) {
			String errorCode = ae.getErrorCode();
			ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(errorCode);
			logger.info("NemID error code: " + errorCode + ", message: " + ae.getMessage() + ", action event: " + actionEvent);
			throw new StatusCodeException(actionEvent, NemIDErrorMapper.getErrorCodeDescription(errorCode), errorCode);
		} catch (StatusCodeException se) {
			ActionEvent actionEvent = se.getActionEvent();
			logger.info("NemID internal error code: " + actionEvent.getCode() + ", message: " + se.getMessage() + ", action event: " + actionEvent);
			throw new StatusCodeException(actionEvent, se.getMessage(), actionEvent.getCode());
		} catch (Throwable t) {
			if (t instanceof Oces2ErrorCode && t.getMessage().equals("CAN003")) {
				throw new StatusCodeException(NemIDActionEvent.STATUS_OTP_TIMEOUT, "OTP timed out due to inactivity");
			}
			EventLogger.dumpStack(t);
			logger.info("Certificate status validation: " + t.getMessage());
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CERT_TYPE_FAILED, "Signature or certificate status validation failed: "
					+ t.getMessage());
		}

		return response;
	}

	private static String getOCSPResponse(X509Certificate[] requestorChain, PrivateKey requestorPrivateKey, X509Certificate certificateToCheck,
			String ocspResponder) {
		try {
			// SSL trust store
			String truststorepath = Config.INSTANCE.getProperty(ConfigKeys.CPR_TRUSTSTORE_PATH);
			String truststorepass = Config.INSTANCE.getProperty(ConfigKeys.CPR_TRUSTSTORE_PASSWORD);

			String proxyHost = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST);
			String proxyPort = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT);
			String proxyUser = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYUSER);
			String proxyPass = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPASS);

			X509Certificate issuerCert = null;
			if ((null != requestorChain) && (requestorChain.length > 1)) {
				issuerCert = requestorChain[1];
			}

			OCSPRequestor ocspRequestor = new OCSPRequestor();
			OCSPData ocspData = ocspRequestor.getCertificateStatus(requestorChain, requestorPrivateKey, certificateToCheck, issuerCert, ocspResponder,
					proxyHost, proxyPort, proxyUser, proxyPass, 10000L, null, truststorepath, truststorepass);

			byte[] bpOCSPResponse = ocspData.getBasicOCSPResponse();
			return StringUtils.stripWhitespaces(new String(Base64.encode(bpOCSPResponse, false), CHARSET_ENCODING));
		} catch (CertificateStatusException ex) {
			logger.info("OCSP response retrieval: " + ex.getMessage());
		} catch (UnsupportedEncodingException ex) {
			logger.info("OCSP response retrieval: " + ex.getMessage());
		}
		return null;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public boolean isValidatedOK() {
		return validatedOK;
	}
	
	/**
	 * Return NemID error codes based on Certificate Status
	 * @param certificateStatus
	 * @throws StatusCodeException
	 */
	private void invalidCertificate (CertificateStatus certificateStatus) throws StatusCodeException {
		
		if (certificateStatus == CertificateStatus.EXPIRED) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_UID_EXPIRED, "Certificate has expired");
		} else if (certificateStatus == CertificateStatus.INVALID) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_UID_INVALID, "Certificate is invalid");
		} else if (certificateStatus == CertificateStatus.NOT_YET_VALID) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_UID_INVALID, "Certificate is not yet valid");
		} else if (certificateStatus == CertificateStatus.REVOKED) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_UID_REVOKED, "Certificate is revoked");
		} else {
			throw new StatusCodeException(NemIDActionEvent.STATUS_UID_INVALID, "Certificate has an unkown error");
		}
	}

}
