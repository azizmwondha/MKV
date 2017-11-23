package no.bbs.trust.ts.idp.nemid.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.transaction.TransactionStatus;

import eu.nets.sis.common.cache.loader.MerchantCache;
import eu.nets.sis.common.cache.types.MerchantProviderConfig;
import no.bbs.trust.amqcapi.MessageQueueProducer;
import no.bbs.trust.amqcapi.constants.AMQConstants;
import no.bbs.trust.amqcapi.message.FinalizeSigningProcessMessage;
import no.bbs.trust.amqcapi.types.AMQAPIException;
import no.bbs.trust.amqcapi.types.QueueMessageEvent;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.RequestReader;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.cprregclientapi.CPRRegistryFacade;
import no.bbs.trust.cprregclientapi.MatchCPR2PIDRequest;
import no.bbs.trust.cprregclientapi.ResponseType;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.trust.ts.idp.nemid.verify.SignatureVerifier;
import no.bbs.trust.ts.idp.nemid.verify.VerifyClientSignatureData;
import no.bbs.trust.ts.idp.nemid.verify.VerifyClientSignatureResponseDataExt;
import no.bbs.trust.ts.rid.client.RIDFacade;
import no.bbs.trust.ts.rid.client.soap.MatchCPRRIDResponse;
import no.bbs.tt.bc.cryptlib.ds.XMLDSIGContent;
import no.bbs.tt.bc.cryptlib.ds.XMLDSIGValidator;
import no.bbs.tt.bc.cryptlib.ds.XMLDSValidationException;
import no.bbs.tt.bc.cryptlib.x509.X509Parser;
import no.bbs.tt.bc.cryptlib.x509.X509ParserException;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;
import no.bbs.tt.trustsign.trustsignDAL.constant.SignerIDTypes;
import no.bbs.tt.trustsign.trustsignDAL.constant.StatusTypes;
import no.bbs.tt.trustsign.trustsignDAL.dao.helpers.MerchantPropertiesHelperDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.BaseorderDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SigningProcessDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.StepDAO;
import no.bbs.tt.trustsign.trustsignDAL.tx.TransactionHelper;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Baseorder;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignerId;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Step;

/**
 *
 */
public class Verify extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private static final String PKICONFIG_PIDSERVICEID = "PIDServiceId";
    private static final String PKICONFIG_RIDSERVICEID = "RIDServiceId";
    private static final String[] SESSION_DATA_KEYS = new String[]{ConfigKeys.SESSIONKEY_SPID, ConfigKeys.SESSIONKEY_CHALLENGE, ConfigKeys.SESSIONKEY_MID,
        "", ""};

    private final TransactionHelper transactionHelper;

    public Verify() {
        transactionHelper = new TransactionHelper();
    }

    @Override
    protected ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
        long start = System.currentTimeMillis();
        logger.info("Verify signature");
        String sref = request.getParameter(ConfigKeys.PARAM_SREF);

        SigningProcess signingProcess;
        TransactionStatus transactionStatus = transactionHelper.getTransaction();
        boolean commit = false;
        Map<String, String> sessionDatas;
        try {
            sessionDatas = DAOUtil.getSessionDataKeysAndValues(sref, SESSION_DATA_KEYS);
            signingProcess = DAOUtil.getSigningProcess((int) StringUtils.toLong(sessionDatas.get(ConfigKeys.SESSIONKEY_SPID), 0));

            // Check signing process, sref and step before proceeding
            checkSigningProcessSrefAndStep(signingProcess, sref);

            String signedResponse = null;
            try {
                signedResponse = request.getParameter("response");
            } catch (IllegalStateException e) {
                //response is to big for java sun server 7, get it from inputstream instead.
                logger.debug("Error when getting response parameter." + e.getMessage(), e);
            }

            if (signedResponse == null) {
                try {
                    signedResponse = extractResponse(request);
                } catch (IOException ioException) {
                    logger.info("Not able to open request input stream to read response");
                    throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_SIGN_FAILED, "Not able to open request input stream for signingProcess: ["
                            + signingProcess.getSignprocessId() + "]" + StatusTypes.getNameById(signingProcess.getStatusId()) + "]");
                }
            }

            logger.debug("Signed response: " + signedResponse);

            VerifyClientSignatureData verifyClientSignatureData = getVerifyClientSignatureData(signingProcess, signedResponse, sessionDatas);
            VerifyClientSignatureResponseDataExt verifyClientSignatureResponseData = verifySign(verifyClientSignatureData);
            verifySignature(verifyClientSignatureResponseData, signingProcess, signedResponse);

            DAOUtil.updateSigningProcessStatus(signingProcess);
            DAOUtil.updateSessionDataByKey(sref, ConfigKeys.SESSIONKEY_STEP, "7");
            commit = true;
        } finally {
            transactionHelper.commitOrRollback(transactionStatus, commit);
        }

        String mid = sessionDatas.get(ConfigKeys.SESSIONKEY_MID);
        sendFinalizeSignProcessMessage(mid, sref, signingProcess);

        // Temporary fix to force a wait for OrderComplete
        try {
            if (MerchantPropertiesHelperDAO.waitForOrderComplete(Integer.parseInt(mid))) {
                waitForOrderCompletion(signingProcess);
            }
        } catch (SQLException e) {
            logger.info("Got exception while checking database for waitForOrderComplete. " + e.getMessage());
        }

        EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_VERIFY_SIGNATURE, start);
        return new ReturnCode(Dispatch.REDIRECT, getConfigProperty(ConfigKeys.CONFIG_NEMID_RECEIPTURL) + "?status=completed&sref=" + sref);
    }

    private static String extractResponse(HttpServletRequest request) throws IOException {
        ServletInputStream inputStream = null;
        String signedResponse = null;
        try {
            inputStream = request.getInputStream();
            String readRequest = RequestReader.readRequest(inputStream);
            if (readRequest.startsWith("response=")) {
                String decodedResponse = URLDecoder.decode(readRequest, no.bbs.trust.ts2.idp.common.util.Constants.CHARSET_UTF8);
                logger.debug(decodedResponse);
                signedResponse = decodedResponse.substring(9);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    inputStream = null;
                }
            }
        }
        return signedResponse;
    }

    private static void checkSigningProcessSrefAndStep(SigningProcess signingProcess, String sref) throws StatusCodeException {
        // Check signing process status before proceeding
        if ((signingProcess.getStatusId() != StatusTypes.READY_ID) && (signingProcess.getStatusId() != StatusTypes.INPROGRESS_ID)) {
            throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_INVALIDSTATUS, "Signing process status is not active [Expected="
                    + StatusTypes.READY_ID + "," + StatusTypes.INPROGRESS_ID + "][Found=" + signingProcess.getStatusId() + "/"
                    + StatusTypes.getNameById(signingProcess.getStatusId()) + "]");
        }

        // Check for valid step
        DAOUtil.validateSessionStep(sref, new int[]{5});

        // Check sref
        if (!sref.equalsIgnoreCase(signingProcess.getSignProcessRef())) {
            logger.info("SignProcess reference updated since session started (OneTimeUrl) [PreviousSREF=" + sref + "][NewSREF="
                    + signingProcess.getSignProcessRef() + "]");
        }
    }

    protected static VerifyClientSignatureData getVerifyClientSignatureData(SigningProcess signingProcess, String signedResponse,
            Map<String, String> sessionDatas) throws StatusCodeException {
        String challenge = sessionDatas.get(ConfigKeys.SESSIONKEY_CHALLENGE);
        logger.debug("Challenge: " + challenge);
        SignObjectData signObject = DAOUtil.getSignObjectData(signingProcess);

        VerifyClientSignatureData vcsdata = new VerifyClientSignatureData();
        vcsdata.setSignature(signedResponse);
        vcsdata.setB64Document(signObject.getObjectB64());
        vcsdata.setChallenge(challenge);

        vcsdata.setCertType(DAOUtil.getCertificateTypes(signingProcess.getSignerId()));

        String mid = sessionDatas.get(ConfigKeys.SESSIONKEY_MID);
        String dtype;
        if (signObject.getElementType().equalsIgnoreCase("pdf")) {
            dtype = "application/pdf";
        } else {
            dtype = "text/plain";
        }
        vcsdata.setDocumentType(dtype);
        vcsdata.setMid(mid);

        SignerId signerID = DAOUtil.getSignerID(signingProcess.getSignerId());
        if (null != signerID) {
            if (SignerIDTypes.SSN.equals(signerID.getIdKey())) {
                vcsdata.setSignerCPR(signerID.getIdValue());
            }
            if (SignerIDTypes.PID.equals(signerID.getIdKey())) {
                vcsdata.setSignerPID(signerID.getIdValue());
            }
            if (SignerIDTypes.RID.equals(signerID.getIdKey())) {
                vcsdata.setSignerRID(signerID.getIdValue());
            }
        }

        return vcsdata;
    }

    private static void verifySignature(VerifyClientSignatureResponseDataExt verifySign, SigningProcess signingProcess, String signedResponse)
            throws StatusCodeException {
        String signerCN;
        String signerCertPolicyOID = null;

        try {
            logger.info("Verify XMLDSIG against signers document");

            byte[] xmldsig = Base64.decode(signedResponse);
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
            logger.debug("Check PID [" + pid + "]");
            DAOUtil.validateSignerID(signingProcess, pid, "PID");
        }
        if (null != verifySign.getSignerRID()) {
            String rid = verifySign.getSignerRID().trim();
            logger.debug("Check RID [" + rid + "]");
            DAOUtil.validateSignerID(signingProcess, rid, "RID");
        }

        logger.info("Verify signer certificate type (check certificate policy)");
        DAOUtil.validateSignerOIDs(signingProcess, signerCertPolicyOID);

        // Status OK
        logger.info("Signature verification completed OK. Store signature and revocation status");

        Date signerTime = new Date();
        String signerIDType = null;
        String signerID = null;
        if (null != verifySign.getSignerCPR()) {
            signerIDType = SignerIDTypes.SSN;
            signerID = verifySign.getSignerCPR();
        } else if (null != verifySign.getSignerPID()) {
            signerIDType = SignerIDTypes.PID;
            signerID = verifySign.getSignerPID();
        } else if (null != verifySign.getSignerPID()) {
            signerIDType = SignerIDTypes.RID;
            signerID = verifySign.getSignerRID();
        }

        DAOUtil.storeSignature(signingProcess.getSignprocessId(), signerTime, signerID, signerCN,
                signerCertPolicyOID, signedResponse, verifySign.getB64ocsp(), signerIDType);
    }

    private static void waitForOrderCompletion(SigningProcess signingProcess) {
        try {
            boolean doneWaiting = false;

            // Get my step
            StepDAO stepDao = new StepDAO();
            Step currStep = stepDao.getByStepId(null, signingProcess.getStepId());

            // Decide if I'm in the latest step
            List<Step> steps = stepDao.getByOrderIdAndMerchantId(null, currStep.getOrderId(), currStep.getMerchantId());
            for (Step step : steps) {
                if (step.getStepNumber() > currStep.getStepNumber()) {
                    doneWaiting = true;
                    break;
                }
            }
            if (!doneWaiting) {
                logger.info("We are in the last step in order");
            } else {
                logger.info("We are not in the last step in order, returning immediately");
                return;
            }

            // Decide if all other signingprocesses in my step is complete
            if (!doneWaiting) {
                SigningProcessDAO signingProcessDao = new SigningProcessDAO();
                List<SigningProcess> spsInStep = signingProcessDao.getByStepId(null, currStep.getStepId());
                for (SigningProcess sp : spsInStep) {
                    if (sp.getSignprocessId() != signingProcess.getSignprocessId() && sp.getStatusId() < 10) {
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
                MerchantProviderConfig mc = MerchantCache.getConfig(Integer.parseInt(mid), PKIIDMap.DKNEMIDJS_ID);

                if (null == mc) {
                    throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant["
                            + mid + "]");
                }

                String serviceId = mc.getString(PKICONFIG_PIDSERVICEID);

                if (!matchCPR2PID(mid, serviceId, signerPID, orderCPR)) {
                    StatusCodeException sce = new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH, "CPR-PID mismatch");
                    StackLogger.logStatusCode(sce);
                    throw sce;
                }
                logger.info("CPR and PID match OK");
                responseData.setSignerCPR(orderCPR);
            } else if (signerRID != null && !signerRID.equals("")) {
                logger.debug("Matching [OrderCPR=" + orderCPR + "] against [SignerRID=" + signerRID + "]");
                MerchantProviderConfig mc = MerchantCache.getConfig(Integer.parseInt(mid), PKIIDMap.DKNEMIDJS_ID);

                if (null == mc) {
                    throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant["
                            + mid + "]");
                }
                if (!matchCPR2RID(mid, signerRID, orderCPR, responseData.certificate)) {
                    throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH, "CPR-RID mismatch");
                }
                logger.info("CPR and RID is matching");
                responseData.setSignerCPR(orderCPR);
            }
        } else {
            logger.info("Order does not contain any CPR requirements, return PID/RID");
            if (signerPID != null && !signerPID.equals("")) {
                responseData.setSignerPID(signerPID);
            } else if (signerRID != null && !signerRID.equals("")) {
                responseData.setSignerRID(signerRID);
            }
        }

        logger.trace("responseData: " + responseData);

        EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_SIGN_OK);
        return responseData;
    }

    private static boolean matchCPR2RID(String mid, String rid, String cpr, byte[] certificate) throws StatusCodeException {
        long start = System.currentTimeMillis();
        logger.debug("Match RID:" + rid + "/oCPR:" + cpr);

        if (StringUtils.isNullorEmpty(cpr) || StringUtils.isNullorEmpty(rid)) {
            return false;
        }

        MerchantProviderConfig mc = MerchantCache.getConfig(Integer.parseInt(mid), PKIIDMap.DKNEMIDJS_ID);

        if (null == mc) {
            throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant[" + mid + "]");
        }

        String serviceid = mc.getString(PKICONFIG_RIDSERVICEID);

        MatchCPRRIDResponse resp;
        try {
            RIDFacade facade = MerchantCache.getRIDFacade(Integer.parseInt(mid), PKIIDMap.DKNEMIDJS_ID);
            resp = facade.matchCPRRID(rid, cpr, certificate, serviceid);
        } catch (Exception e) {
            logger.warn("Unable to do a rid match for [MerchantID=" + mid + "] Errormessage: " + e.getMessage());
            throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Unable to do a rid match for [MerchantID=" + mid + "]");
        } finally {
            EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_RID_MATCH, start);
        }

        logger.info("[MatchStatus=" + resp.getStatuscode() + "][MatchMessage=" + resp.getStatustext() + "]");

        if (resp.getStatuscode() == 0) {
            EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_RIDMATCH);
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

    private static boolean matchCPR2PID(String mid, String nemIDserviceID, String pid, String cpr) throws StatusCodeException {
        logger.debug("Match sPID:" + pid + "/oCPR:" + cpr);

        if (StringUtils.isNullorEmpty(cpr) || StringUtils.isNullorEmpty(pid)) {
            return false;
        }

        int serviceId = (int) StringUtils.toLong(nemIDserviceID, 0);

        CPRRegistryFacade facade = MerchantCache.getCPRRegistryFacade(Integer.parseInt(mid), PKIIDMap.DKNEMIDJS_ID);

        int requestId = (int) (Math.random() * Integer.MAX_VALUE);

        logger.debug("CPRRequest ID: " + requestId);

        ResponseType resp;
        try {
            MatchCPR2PIDRequest match = new MatchCPR2PIDRequest(serviceId, pid, cpr, requestId);
            resp = facade.sendCPRRegistryRequest(match);
        } catch (Exception e) {
            logger.warn("Unable to do a pid match for [MerchantID=" + mid + "] Errormessage: " + e.getMessage());
            throw new StatusCodeException(NemIDActionEvent.STATUS_RID_LOOKUP_FAILED, "Unable to do a pid match for [MerchantID=" + mid + "]", e);
        }
        EventLogger.appendEvent(NemIDActionEvent.ACTION_DK_NEMID_CPRMATCH);
        logger.info("[MatchStatus=" + resp.getStatus().getStatusCode() + "][MatchMessage=" + resp.getStatus().getStatusText().get(0).getValue() + "]");
        return (resp.getStatus().getStatusCode() == 0);
    }

    private static void sendFinalizeSignProcessMessage(String mid, String sref, SigningProcess signingProcess) throws StatusCodeException {
        String orderId = DAOUtil.getOrderID(signingProcess);

        QueueMessageEvent queueMessageEvent = null;
        MessageQueueProducer messageQueueProducer;
        try {
            messageQueueProducer = new MessageQueueProducer(AMQConstants.QUEUE_FINALIZE_SP);

            FinalizeSigningProcessMessage message = new FinalizeSigningProcessMessage(sref, orderId, mid, signingProcess.getSignprocessId());
            queueMessageEvent = message.toMessageEvent();
            logger.info("Registering [Event=" + queueMessageEvent + "]");

            messageQueueProducer.sendMessage(queueMessageEvent);
        } catch (AMQAPIException exp) {
            logger.fatal("Unable to register [QueueEvent=" + queueMessageEvent + "] - " + exp.getMessage());
            EventLogger.dumpStack(exp);
            throw new StatusCodeException(NemIDActionEvent.STATUS_AMQ_ERROR, "Unable to register [QueueEvent=" + queueMessageEvent + "] Reason"
                    + exp.getMessage());
        }
    }
}
