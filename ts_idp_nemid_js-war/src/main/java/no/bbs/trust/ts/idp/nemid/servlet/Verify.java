package no.bbs.trust.ts.idp.nemid.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.amqcapi.AMQAPIException;
import no.bbs.trust.amqcapi.MessageQueueProducer;
import no.bbs.trust.amqcapi.QueueMessageEvent;
import no.bbs.trust.amqcapi.constants.AMQConstants;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.cprregclientapi.CPRRegistryFacade;
import no.bbs.trust.cprregclientapi.MatchCPR2PIDRequest;
import no.bbs.trust.cprregclientapi.ResponseType;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.contants.Constants;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.trust.ts.idp.nemid.verify.SignatureVerifier;
import no.bbs.trust.ts.idp.nemid.verify.VerifyClientSignatureData;
import no.bbs.trust.ts.idp.nemid.verify.VerifyClientSignatureResponseDataExt;
import no.bbs.trust.ts.rid.client.RIDFacade;
import no.bbs.trust.ts.rid.client.RIDFacadeFactory;
import no.bbs.trust.ts.rid.client.RIDMerchantContext;
import no.bbs.trust.ts.rid.client.soap.MatchCPRRIDResponse;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContext;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.bc.cryptlib.ds.XMLDSIGContent;
import no.bbs.tt.bc.cryptlib.ds.XMLDSIGValidator;
import no.bbs.tt.bc.cryptlib.ds.XMLDSValidationException;
import no.bbs.tt.bc.cryptlib.x509.X509Parser;
import no.bbs.tt.bc.cryptlib.x509.X509ParserException;
import no.bbs.tt.trustsign.te.common.constants.TEConstants;
import no.bbs.tt.trustsign.trustsignDAL.constant.DbTableInfo;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.constant.StatusTypes;
import no.bbs.tt.trustsign.trustsignDAL.dao.helpers.MerchantPropertiesHelperDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.BaseorderDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SigningProcessDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.StepDAO;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Baseorder;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignerId;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Step;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author azm
 */
public class Verify extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private static final String PKICONFIG_PIDSERVICEID = "PIDServiceId";
	private static final String PKICONFIG_RIDSERVICEID = "RIDServiceId";

	@Override
	protected ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		logger.info("Verify signature");

		String signedResponse = request.getParameter("response");
		logger.debug("Signed response: " + signedResponse);

		String sref = request.getParameter(ConfigKeys.PARAM_SREF);
		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		SigningProcess signingProcess = DAOUtil.getSigningProcess(spid);

		// Check signing process, sref and step before proceeding
		checkSigningProcessSrefAndStep(signingProcess, sref);

		String result = request.getParameter("result");
		logger.debug("Result: " + result);
		//		if (!("" + result).equalsIgnoreCase("b2s=")) { // ok
		//			if (("" + result).equalsIgnoreCase("Y2FuY2Vs")) { // cancel
		//				return new ReturnCode(Dispatch.REDIRECT, getConfigProperty(ConfigKeys.CONFIG_NEMID_CANCELURL) + "?status=cancel&sref=" + sref);
		//			}
		//			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Signature verification failed. Reason: "
		//					+ new String(Base64.decode("" + result)));
		//		}

		String signature = request.getParameter("signature");
		signature = signedResponse;
		String challenge = request.getParameter("challenge");

		byte[] xmldsig = Base64.decode(signature);

		SignObjectData signObject = DAOUtil.getSignObjectData(signingProcess);

		VerifyClientSignatureData vcsdata = new VerifyClientSignatureData();
		vcsdata.setSignature(signature);
		vcsdata.setB64Document(signObject.getObjectB64());
		vcsdata.setChallenge(challenge);

		vcsdata.setCertType(DAOUtil.getCertificateTypes(signingProcess.getSignerId()));

		String mid = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_MID);
		String dtype = null;
		if (signObject.getElementType().equalsIgnoreCase("pdf")) {
			dtype = "application/pdf";
		} else {
			dtype = "text/plain";
		}
		vcsdata.setDocumentType(dtype);
		vcsdata.setMid(mid);
		//		vcsdata.setResult(result);

		SignerId signerID = DAOUtil.getSignerID(signingProcess.getSignerId());
		if (null != signerID) {
			if ("SSN".equalsIgnoreCase("" + signerID.getIdKey())) {
				vcsdata.setSignerCPR(signerID.getIdValue());
			}
			if ("PID".equalsIgnoreCase("" + signerID.getIdKey())) {
				vcsdata.setSignerPID(signerID.getIdValue());
			}
			if ("RID".equalsIgnoreCase("" + signerID.getIdKey())) {
				vcsdata.setSignerRID(signerID.getIdValue());
			}
		}

		logger.info("Verify XMLDSIG signature");
		VerifyClientSignatureResponseDataExt verifySign = verifySign(vcsdata);

		String signerCN = null;
		String signerCertPolicyOID = null;

		try {
			logger.info("Verify XMLDSIG against signers document");

			XMLDSIGValidator xdvalidator = new XMLDSIGValidator();
			XMLDSIGContent xdcontent = xdvalidator.validateXMLDSig(xmldsig);

			xdcontent.getNumberOfSignedObjects();

			X509Certificate signerCert = xdcontent.getSignerCertificate();
			X509Parser xp = new X509Parser(signerCert);
			signerCN = xp.getSubjectCommonName();

			String[] oids = xp.getPolicyIdentifiers();
			if ((null != oids) && (oids.length > 0)) {
				StringBuilder sb = new StringBuilder();

				for (String oid : oids) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(oid);
				}
				signerCertPolicyOID = sb.toString();
			}
			logger.info("Signer info [SignerOID=" + signerCertPolicyOID + "][SignerCN=" + signerCN + "]");
			logger.debug("Signer info [SignerCPR=" + verifySign.getSignerCPR() + "][SignerPID=" + verifySign.getSignerPID() + "][SignerRID="
					+ verifySign.getSignerRID() + "]");
		} catch (InstantiationException ie) {
			EventLogger.dumpStack(ie);
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Cannot parse signature. Reason: " + ie.getMessage());
		} catch (X509ParserException xpe) {
			EventLogger.dumpStack(xpe);
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Cannot parse signer certificate. Reason: " + xpe.getMessage());
		} catch (XMLDSValidationException xve) {
			EventLogger.dumpStack(xve);
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Cannot verify XML signature. Reason: " + xve.getMessage());
		} catch (Throwable t) {
			EventLogger.dumpStack(t);
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Cannot verify XML signature. Reason: " + t.getMessage());
		}

		// Verify signer ID/SSN
		logger.info("Verify signer ID (check CPR, PID, RID)");
		DAOUtil.validateSignerID(signingProcess, verifySign.getSignerCPR(), "SSN");

		if (null != verifySign.getSignerPID()) {
			String pid = "PID:" + verifySign.getSignerPID();
			pid = pid.substring(pid.lastIndexOf(":") + 1);
			logger.debug("Check PID[" + pid + "]");
			DAOUtil.validateSignerID(signingProcess, pid, "PID");
		}
		if (null != verifySign.getSignerRID()) {
			String rid = verifySign.getSignerRID().trim();
			logger.debug("Check RID[" + rid + "]");
			DAOUtil.validateSignerID(signingProcess, rid, "RID");
		}

		logger.info("Verify signer certificate type (check certificate policy)");
		DAOUtil.validateSignerOIDs(signingProcess, signerCertPolicyOID);

		// Status OK
		logger.info("Signature verification completed OK. Store signature and revocation status");

		Date signerTime = new Date();
		DAOUtil.storeSignature(spid, signerTime, verifySign.getSignerCPR(), signerCN, signerCertPolicyOID, signature, verifySign.getB64ocsp());
		DAOUtil.updateSigningProcessStatus(spid);
		notifyTE(mid, sref, signingProcess);
		DAOUtil.updateSessionDataByKey(sref, ConfigKeys.SESSIONKEY_STEP, "7");

		// Temporary fix to force a wait for OrderComplete
		try {
			if (MerchantPropertiesHelperDAO.waitForOrderComplete(Integer.parseInt(mid))) {
				waitForOrderCompletion(spid);
			}
		} catch (SQLException e) {
			logger.info("Got exception while checking database for waitForOrderComplete. " + e.getMessage());
		}

		EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_VERIFY_SIGNATURE, start);
		return new ReturnCode(Dispatch.REDIRECT, getConfigProperty(ConfigKeys.CONFIG_NEMID_RECEIPTURL) + "?status=completed&sref=" + sref);
	}

	private static void checkSigningProcessSrefAndStep(SigningProcess signingProcess, String sref) throws StatusCodeException {
		// Check signing process status before proceeding
		if ((signingProcess.getStatusId() != StatusTypes.READY_ID) && (signingProcess.getStatusId() != StatusTypes.INPROGRESS_ID)) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_INVALIDSTATUS, "Signing process status is not active [Expected="
					+ StatusTypes.READY_ID + "," + StatusTypes.INPROGRESS_ID + "][Found=" + signingProcess.getStatusId() + "/"
					+ StatusTypes.getNameById(signingProcess.getStatusId()) + "]");
		}

		// Check for valid step
		DAOUtil.validateSessionStep(sref, new int[] { 5 });

		// Check sref
		if (!sref.equalsIgnoreCase(signingProcess.getSignProcessRef())) {
			logger.info("SignProcess reference updated since session started (OneTimeUrl) [PreviousSREF=" + sref + "][NewSREF="
					+ signingProcess.getSignProcessRef() + "]");
		}
	}

	private static void waitForOrderCompletion(int spid) {
		try {
			boolean doneWaiting = false;
			// Get my signingprocess
			SigningProcessDAO spd = new SigningProcessDAO();
			SigningProcess sp = spd.getBySignprocessid(null, spid);

			// Get my step
			StepDAO stepDao = new StepDAO();
			Step currStep = stepDao.getByStepId(null, sp.getStepId());

			// Decide if I'm in the latest step
			ArrayList<Step> steps = stepDao.getByOrderIdAndMerchantId(null, currStep.getOrderId(), currStep.getMerchantId());
			for (Step step : steps) {
				if (step.getStepNumber() > currStep.getStepNumber()) {
					doneWaiting = true;
					break;
				}
			}
			if (!doneWaiting) {
				logger.info("We are in the last step in order");
			} else {
				logger.info("We are not in the last step in order, returning imediately");
				return;
			}

			// Decide if all other signingprocesses in my step is complete
			if (!doneWaiting) {
				ArrayList<SigningProcess> spsInStep = spd.getByStepId(null, currStep.getStepId());
				for (SigningProcess signingProcess : spsInStep) {
					if (signingProcess.getSignprocessId() != sp.getSignprocessId() && signingProcess.getStatusId() < 10) {
						doneWaiting = true;
						break;
					}
				}
			}
			if (!doneWaiting) {
				logger.info("All other signingprocesses are completed");
			} else {
				logger.info("There are other uncompleted signingprocesses, returning imediately");
				return;
			}

			// Wait until order is completed
			BaseorderDAO bo = new BaseorderDAO();
			long start = System.currentTimeMillis();
			while (!doneWaiting) {
				Baseorder baseOrder = bo.getByOrderIdMerchantid(null, currStep.getOrderId(), currStep.getMerchantId());
				doneWaiting = baseOrder.getStatusId() >= 10;
				logger.info("Checked baseorder [orderstatus=" + baseOrder.getStatusId() + "] [donewaiting=" + doneWaiting + "]");
				if (doneWaiting) {
					return;
				}
				try {
					Thread.sleep(250);
				} catch (InterruptedException ie) {
					logger.info("Got interrupted while sleeping");
				}
				if (System.currentTimeMillis() - start > 8000) {
					doneWaiting = true;
					logger.info("Waited for over 8 sec, giving up");
				}
			}
		} catch (Exception e) {
			logger.warn("Got exception waiting for order to be completed: " + e.getMessage());
		}
	}

	private static VerifyClientSignatureResponseDataExt verifySign(VerifyClientSignatureData verifyClientSignatureData) throws StatusCodeException {
		try {
			SignatureVerifier signatureVerifier = new SignatureVerifier(verifyClientSignatureData);

			VerifyClientSignatureResponseDataExt responseData = signatureVerifier.handleVerifyClientSignatureData();

			String mid = verifyClientSignatureData.getMid();
			responseData.setMid(mid);
			responseData.setTime(verifyClientSignatureData.getTime());
			responseData.setTransref(verifyClientSignatureData.getTransref());
			responseData.setB64signature(verifyClientSignatureData.getSignature());

			String signerPID = responseData.getSignerPID();
			String signerRID = responseData.getSignerRID();
			String orderPID = verifyClientSignatureData.getSignerPID();
			String orderRID = verifyClientSignatureData.getSignerRID();
			String orderCPR = verifyClientSignatureData.getSignerCPR();

			logger.debug("SignerPID: " + signerPID + "] IsNullOrEmpty=" + StringUtils.isNullorEmpty(signerPID));
			logger.debug("SignerRID: " + signerRID + "] IsNullOrEmpty=" + StringUtils.isNullorEmpty(signerRID));
			logger.debug("OrderPID: [" + orderPID + "] IsNullOrEmpty=" + StringUtils.isNullorEmpty(orderPID));
			logger.debug("OrderRID: [" + orderRID + "] IsNullOrEmpty=" + StringUtils.isNullorEmpty(orderRID));
			logger.debug("OrderCPR: [" + orderCPR + "] IsNullOrEmpty=" + StringUtils.isNullorEmpty(orderCPR));
			logger.debug("OrderCertPolicy: " + verifyClientSignatureData.getCertType());

			if (verifyClientSignatureData.getCertType() != null && verifyClientSignatureData.getCertType().equalsIgnoreCase("Personal")) {
				if (signerPID == null || signerPID.equals("")) {
					throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CERT_TYPE_FAILED, "Expected POCES but signature cert was MOCES");
				}
			} else if (verifyClientSignatureData.getCertType() != null && verifyClientSignatureData.getCertType().equalsIgnoreCase("Employee")) {
				if (signerRID == null || signerRID.equals("")) {
					throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CERT_TYPE_FAILED, "Expected MOCES but signature cert was POCES");
				}
			}

			if (!StringUtils.isNullorEmpty(orderCPR)) {
				if (signerPID != null && !signerPID.equals("")) {
					logger.debug("Matching [OrderCPR=" + orderCPR + "] against [SignerPID=" + signerPID + "]");
					MerchantContext mc = MerchantContextCache.getMerchantContext(mid);

					if (null == mc) {
						throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant["
								+ mid + "]");
					}

					Map<String, String> idpc = mc.getIdpConfig();
					String serviceId = idpc.get(PKICONFIG_PIDSERVICEID);

					if (!matchCPR2PID(mid, serviceId, signerPID, orderCPR)) {
						StatusCodeException sce = new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH, "CPR-PID mismatch");
						StackLogger.logStatusCode(sce);
						throw sce;
					}
					logger.info("CPR and PID match OK");
					responseData.setSignerCPR(orderCPR);
				} else if (signerRID != null && !signerRID.equals("")) {
					logger.debug("Matching [OrderCPR=" + orderCPR + "] against [SignerRID=" + signerRID + "]");
					MerchantContext mc = MerchantContextCache.getMerchantContext(mid);

					if (null == mc) {
						throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant["
								+ mid + "]");
					}
					if (!matchCPR2RID(mid, signerRID, orderCPR, responseData.certificate)) {
						StatusCodeException sce = new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH, "CPR-RID mismatch");
						StackLogger.logStatusCode(sce);
						throw sce;
					}
					logger.info("CPR and RID is matching");
					responseData.setSignerCPR(orderCPR);
				}
			} else {
				logger.info("Order does not contain any requirements on PID/RID/CPR");
			}

			logger.trace("responseData: " + responseData);

			EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_SIGN_OK);
			return responseData;
		} catch (StatusCodeException sce) {
			throw sce;
		} catch (Throwable t) {
			EventLogger.dumpStack(t);
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, t.getMessage());
		}
	}

	private static boolean matchCPR2RID(String mid, String rid, String cpr, byte[] certificate) throws StatusCodeException {
		long start = System.currentTimeMillis();
		logger.debug("Match RID:" + rid + "/oCPR:" + cpr);

		if (StringUtils.isNullorEmpty(cpr) || StringUtils.isNullorEmpty(rid)) {
			return false;
		}
		RIDMerchantContext ctx = new RIDMerchantContext();

		String truststorepath = Config.INSTANCE.getProperty(ConfigKeys.RID_TRUSTSTORE_PATH);
		String truststorepass = Config.INSTANCE.getProperty(ConfigKeys.RID_TRUSTSTORE_PASSWORD);
		String truststoretype = Config.INSTANCE.getProperty(ConfigKeys.RID_TRUSTSTORE_TYPE);

		String lookupURL = Config.INSTANCE.getProperty(ConfigKeys.RIDREG_LOOKUP_URL);
		String proxyHost = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST);
		String proxyPort = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT);

		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("truststorepath: " + truststorepath);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).trace("truststorepass: " + truststorepass);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("truststoretype: " + truststoretype);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("lookupURL: " + lookupURL);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("proxyHost: " + proxyHost);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("proxyPort: " + proxyPort);

		MerchantContext mc = MerchantContextCache.getMerchantContext(mid);

		if (null == mc) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant[" + mid + "]");
		}

		Map<String, String> idpc = mc.getIdpConfig();
		String keystorepath = idpc.get(PKIConfigKeys.RIDKEYSTOREPATH);
		String keystorepass = idpc.get(PKIConfigKeys.RIDKEYSTOREPWD);
		String keystoretype = idpc.get(PKIConfigKeys.RIDKEYSTORETYPE);
		String serviceid = idpc.get(PKICONFIG_RIDSERVICEID);

		if (keystoretype == null || keystoretype.equals("") || keystorepass == null || keystorepass.equals("")) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_SETUP_FAIL, "Unable to find rid keystore for Merchant[" + mid + "]");
		}

		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("CPRRequest keystore: " + keystorepath);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("CPRRequest sto type: " + keystoretype);

		ctx.setTruststore(truststorepath);
		ctx.setTruststorePwd(truststorepass);
		ctx.setTruststoreType(truststoretype);
		ctx.setProxyHost(proxyHost);
		ctx.setProxyPort(proxyPort);
		ctx.setRidServiceURL(lookupURL);
		ctx.setKeystore(keystorepath);
		ctx.setKeystorePwd(keystorepass);
		ctx.setKeystoreType(keystoretype);

		MatchCPRRIDResponse resp;
		try {
			RIDFacade facade = RIDFacadeFactory.getRIDFacade(ctx);
			resp = facade.matchCPRRID(rid, cpr, certificate, serviceid);
		} catch (Exception e) {
			logger.error("Unable to do a rid match for [Merchant=" + mid + "] Errormessage: " + e.getMessage());
			throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Unable to do a rid match for [MerchantID=" + mid + "]");
		} finally {
			EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_RID_MATCH, start);
		}

		EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_RIDMATCH);
		logger.info("[MatchStatus=" + resp.getStatuscode() + "][MatchMessage=" + resp.getStatustext() + "]");

		if (resp.getStatuscode() == 0) {
			return resp.isMatch();
		} else if (resp.getStatuscode() == 4) {
			logger.warn("Validation failed in ridregister: [ridstatuscode=" + resp.getStatuscode() + "] [ridstatusmessage=" + resp.getStatustext()
					+ "] The probable cause is wrong serviceid. [ridserviceid=" + serviceid + "][mid=" + mid + "]");
			throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Error in rid match");
		} else if (resp.getStatuscode() == 8) {
			logger.info("Validation failed in ridregister: [ridstatuscode=" + resp.getStatuscode() + "][ridstatusmessage=" + resp.getStatustext()
					+ "] This might happen if cert is not connected to a cpr/ssn");
			throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Error in rid match");
		} else {
			logger.info("Validation failed in ridregister: [ridstatuscode=" + resp.getStatuscode() + "][ridstatusmessage=" + resp.getStatustext()
					+ "] Reason is unknown.");
			throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Error in rid match");
		}
	}

	private static boolean matchCPR2PID(String mid, String nemIDserviceID, String pid, String cpr) throws Exception {
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("Match sPID:" + pid + "/oCPR:" + cpr);

		if (StringUtils.isNullorEmpty(cpr) || StringUtils.isNullorEmpty(pid)) {
			return false;
		}

		String truststorepath = Config.INSTANCE.getProperty(ConfigKeys.CPR_TRUSTSTORE_PATH);
		String truststorepass = Config.INSTANCE.getProperty(ConfigKeys.CPR_TRUSTSTORE_PASSWORD);
		String truststoretype = Config.INSTANCE.getProperty(ConfigKeys.CPR_TRUSTSTORE_TYPE);
		String lookupURL = Config.INSTANCE.getProperty(ConfigKeys.CPRREG_LOOKUP_URL);
		String proxyHost = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST);
		String proxyPort = Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT);

		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("truststorepath: " + truststorepath);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).trace("truststorepass: " + truststorepass);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("truststoretype: " + truststoretype);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("lookupURL: " + lookupURL);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("proxyHost: " + proxyHost);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("proxyPort: " + proxyPort);

		MerchantContext mc = MerchantContextCache.getMerchantContext(mid);

		if (null == mc) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant[" + mid + "]");
		}

		java.util.Map<String, String> idpc = mc.getIdpConfig();

		String keystorepath = idpc.get(PKIConfigKeys.KEYSTORE);
		String keystorepass = idpc.get(PKIConfigKeys.KEYSTORE_PASSWORD);
		String keystoretype = (keystorepath.toLowerCase().endsWith(".p12")) ? "PKCS12" : "JKS";

		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("CPRRequest keystore: " + keystorepath);

		int serviceId = (int) StringUtils.toLong(nemIDserviceID, 0);
		int VOID_MERCHANT_ID = 127;
		CPRRegistryFacade facade = new CPRRegistryFacade(VOID_MERCHANT_ID, serviceId, keystorepath, keystorepass, keystoretype, truststorepath, truststorepass,
				truststoretype, lookupURL, 5000, proxyHost, proxyPort, null, null);

		int requestId = (int) (Math.random() * Integer.MAX_VALUE);

		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).debug("CPRRequest ID: " + requestId);

		MatchCPR2PIDRequest match;
		try {
			match = new MatchCPR2PIDRequest(serviceId, pid, cpr, requestId);
		} catch (Exception e) {
			Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).error(
					"Unable to do a pid match for [Merchant=" + mid + "] Errormessage: " + e.getMessage());
			throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Unable to do a pid match for Merchant[" + mid + "]");
		}
		ResponseType resp = facade.sendCPRRegistryRequest(match);
		EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_CPRMATCH);
		Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).info(
				"[MatchStatus=" + resp.getStatus().getStatusCode() + "][MatchMessage=" + resp.getStatus().getStatusText().get(0).getValue() + "]");
		return (resp.getStatus().getStatusCode() == 0);
	}

	private void notifyTE(String mid, String sref, SigningProcess sp) throws StatusCodeException {
		String orderID = DAOUtil.getOrderID(sp);

		QueueMessageEvent qme = null;
		MessageQueueProducer mqProducer = null;
		try {
			mqProducer = new MessageQueueProducer(AMQConstants.QUEUE_FINALIZE_SP, getConfigProperty(Constants.ACTIVEMQ_URL));
			qme = new QueueMessageEvent();
			qme.put(Constants.TID, sref);
			qme.put(DbTableInfo.BO_ORDERID, URLEncoder.encode(orderID, TEConstants.CHARSET_UTF8));
			qme.put(DbTableInfo.BO_MERCHANTID, mid);
			qme.put(DbTableInfo.SIGPRO_SIGNPROSID, "" + sp.getSignprocessId());
			logger.info("Registering [Event=" + qme + "]");

			mqProducer.sendMessage(qme);
			mqProducer.close();
		} catch (AMQAPIException exp) {
			logger.fatal("Unable to register [QueueEvent=" + qme + "] - " + exp.getMessage());
			EventLogger.dumpStack(exp);
			throw new StatusCodeException(NemIDActionEvent.STATUS_AMQ_ERROR, "Unable to register [QueueEvent=" + qme + "] Reason" + exp.getMessage());
		} catch (UnsupportedEncodingException exp) {
			logger.fatal("Error during queue message registration [QueueEvent=" + qme + "] - " + exp.getMessage());
			EventLogger.dumpStack(exp);
			throw new StatusCodeException(NemIDActionEvent.STATUS_ENCODING_ERROR, "Unable to encode order ID [" + orderID + "] Reason" + exp.getMessage());
		} finally {
			if (mqProducer != null) {
				mqProducer.close();
			}
		}
	}

	@Override
	public void doInit() throws ServletException {
		//		throw new UnsupportedOperationException("Not supported yet.");
	}

}
