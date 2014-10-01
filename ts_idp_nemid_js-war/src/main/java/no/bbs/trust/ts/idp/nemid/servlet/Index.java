/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.servlet;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.nets.no.vas.esign.sdosigner.types.KeyCredentials;
import no.bbs.trust.common.basics.charset.Charsets;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.i18n.LangSupport;
import no.bbs.trust.ts.idp.nemid.attachments.Attachment;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.contants.Constants;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;
import no.bbs.trust.ts.idp.nemid.tag.ChallengeGenerator;
import no.bbs.trust.ts.idp.nemid.tag.OcesJsonParameterGenerator;
import no.bbs.trust.ts.idp.nemid.tag.Signer;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.tt.bc.cryptlib.util.HashUtil;
import no.bbs.tt.trustsign.te.xml.messages.ErrorResponse;
import no.bbs.tt.trustsign.te.xml.messages.GetStatusTableRequest;
import no.bbs.tt.trustsign.te.xml.messages.GetStatusTableResponse;
import no.bbs.tt.trustsign.te.xml.messages.SignerStatusTable;
import no.bbs.tt.trustsign.te.xml.messages.TEMessage;
import no.bbs.tt.trustsign.tecapi.communicator.Requestor;
import no.bbs.tt.trustsign.trustsignDAL.constant.SessionKey;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SessionDataDAO;
import no.bbs.tt.trustsign.trustsignDAL.tx.TransactionHelper;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObject;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.WebContext;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.openoces.ooapi.utils.Base64Handler;
import org.springframework.transaction.TransactionStatus;

/**
 */
public class Index extends BaseServlet {

	private static final long serialVersionUID = 1L;

	public static final String COMPONENT_NAME = "NemIDJS";
	private static final String[] SESSION_DATA_KEYS = new String[] { ConfigKeys.SESSIONKEY_SPID, ConfigKeys.SESSIONKEY_MID, ConfigKeys.SESSIONKEY_LOCALE,
		ConfigKeys.SESSIONKEY_TZO, ConfigKeys.SESSIONKEY_NEMID_CLIENTMODE };

	private Map<String, String> sessionDatas;

	private final TransactionHelper transactionHelper;

	public Index() {
		transactionHelper = new TransactionHelper();
	}

	@Override
	protected ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		logger.info("Get NemID client activation tag");
		String sref = request.getParameter(ConfigKeys.PARAM_SREF);

		TransactionStatus tx = transactionHelper.getTransaction();
		boolean commit = false;
		try {
			DAOUtil.validateSessionStep(sref, new int[] { 2, 4, 5, 6 });
			sessionDatas = DAOUtil.getSessionDataKeysAndValues(sref, SESSION_DATA_KEYS);

			String clientMode = getClientMode(request);
			int spid = (int) StringUtils.toLong(sessionDatas.get(ConfigKeys.SESSIONKEY_SPID), 0);
			SigningProcess signingProcess = DAOUtil.getSigningProcess(spid);

			if (!sref.equalsIgnoreCase(signingProcess.getSignProcessRef())) {
				logger.info("SignProcess reference updated since session started (OneTimeUrl) [PreviousSREF=" + sref + "][NewSREF="
						+ signingProcess.getSignProcessRef() + "]");
			}

			DAOUtil.updateSessionDataByKey(sref, ConfigKeys.SESSIONKEY_STEP, "5");

			String mid = sessionDatas.get(ConfigKeys.SESSIONKEY_MID);
			String locale = sessionDatas.get(ConfigKeys.SESSIONKEY_LOCALE);
			locale = (locale.trim().length() > 0) ? locale : LangSupport.getDefaultLanguage();
			request.setAttribute("locale", locale);
			String languageCode;
			try {
				languageCode = new SessionDataDAO().getBySrefAndKey(sref, SessionKey.LOCALE).getVal();
			} catch (SQLException e) {
				throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, e, COMPONENT_NAME, sref);
			}

			OcesJsonParameterGenerator clientGenerator = createClientGenerator(mid);
			setSigningDocument(clientGenerator, signingProcess, sref);
			String challenge = Base64Handler.encode(ChallengeGenerator.generateChallenge());
			String nemidTag = clientGenerator.generateClientTag(clientMode, languageCode, challenge, sref);
			String clientTag = String.format(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTTAG_DIV), " " + clientMode, nemidTag);
			logger.debug("NemID JS client tag: " + clientTag);
			request.setAttribute("clienttag", clientTag);
			DAOUtil.updateSessionDataByKey(sref, ConfigKeys.SESSIONKEY_CHALLENGE, challenge);

			setupWebContext(sref, signingProcess, request);
			request.setAttribute("sref", sref);

			// Parse signer deadline
			long endtime = signingProcess.getDeadline().getTime();

			SimpleDateFormat formatter = new SimpleDateFormat(LangSupport.getUserText("format.mediumdate", locale));
			String tzos = sessionDatas.get(ConfigKeys.SESSIONKEY_TZO);
			tzos = (tzos.trim().length() > 0) ? tzos : "0";
			long tzo = Long.parseLong(tzos);

			request.setAttribute("signstatus.deadline", formatter.format(new Date(endtime + tzo)));
			request.setAttribute("tzo", tzos);

			try {
				request.setAttribute("statustable", getStatusTable(mid, sref, signingProcess));
			} catch (StatusCodeException sce) {
				logger.info("No status table available for SREF");
			}
			EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_GENERATE_CLIENT_TAG, start);
			commit = true;
			return new ReturnCode(Dispatch.INCLUDE, "/index.jsp");
		} finally {
			transactionHelper.commitOrRollback(tx, commit);
		}
	}

	String getClientMode(HttpServletRequest request) {
		String clientMode = request.getParameter(ConfigKeys.PARAM_NEMID_CLIENTMODE);
		if ((clientMode == null || "".equals(clientMode)) && sessionDatas != null) {
			clientMode = sessionDatas.get(ConfigKeys.SESSIONKEY_NEMID_CLIENTMODE);
		}

		// Default to standard mode if no valid value is given
		if (!Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD).equals(clientMode)
				&& !Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_LIMITED).equals(clientMode)) {
			clientMode = Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD);
		}
		return clientMode;
	}

	static OcesJsonParameterGenerator createClientGenerator(String mid) throws StatusCodeException {
		KeyCredentials credentials;
		try {
			credentials = DAOUtil.getMerchantCredentials(mid);
		} catch (SQLException ex) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to obtain key credentials for Merchant [" + mid + "] "
					+ ex.getMessage());
		}

		Signer signer = new Signer(credentials.getKeystorepath(), credentials.getKeystorepass(), credentials.getKeyalias(), credentials.getKeyaliaspass());
		return new OcesJsonParameterGenerator(signer);
	}

	static void setSigningDocument(OcesJsonParameterGenerator clientGenerator, SigningProcess signingProcess, String sref) throws StatusCodeException {
		SignObjectData signObjectData = DAOUtil.getSignObjectData(signingProcess);
		SignObject signObject = DAOUtil.getSignObject(signObjectData.getSignerObjectId());
		String docType = signObjectData.getElementType();
		String docTitle = signObject.getTitle();
		String docDescription = signObject.getDescription();
		logger.debug("Doc type: " + docType + ", title: " + docTitle + ", description: " + docDescription);

		if ("txt,text,text/plain".contains(docType.toLowerCase())) {
			docType = "text";
			String signText = new String(Base64.decode(signObjectData.getObjectB64()), Charsets.UTF_8);
			logger.debug("Document signText: " + signText);
			clientGenerator.setSignText(signText, docType);
		} else {
			Attachment attachment = new Attachment();
			attachment.setTitle(docTitle);
			attachment.setMimeType(docType);

			byte[] documentBytes = Base64.decode(signObjectData.getObjectB64());
			try {
				String docHash = new String(Base64.encode(HashUtil.hash(documentBytes, Constants.DIGEST_SHA2)), Charsets.UTF_8);
				attachment.setB64HashValue(docHash);
				attachment.setB64HashAlgo(Constants.DIGEST_SHA2_SHORTNAME);
			} catch (NoSuchAlgorithmException e) {
				Logger.getLogger(Index.class.getName()).error(e);
			} catch (NoSuchProviderException e) {
				Logger.getLogger(Index.class.getName()).error(e);
			}

			String docUrl = getConfigProperty(ConfigKeys.CONFIG_NEMID_DOCURL) + "?" + ConfigKeys.PARAM_SREF + "=" + sref;
			attachment.setPath(docUrl);
			attachment.setSize(documentBytes.length);

			logger.debug("Attachment: " + attachment.toXML());
			clientGenerator.setSignPdf(docDescription, attachment);
		}
	}

	private static void setupWebContext(String tid, SigningProcess sp, HttpServletRequest request) throws StatusCodeException {
		WebContext wc = DAOUtil.getWebContext(sp.getWebcontextid());

		// Web context and merchant URLs
		updateWCattributes(ConfigKeys.SESSIONKEY_STYLE, "styleurl", wc.getStyleUrl(), tid, request);

		// SE_BID application URLs
		request.setAttribute("verifyurl", getConfigProperty(ConfigKeys.CONFIG_NEMID_TAG_VERIFYURL));
		request.setAttribute("docurl", getConfigProperty(ConfigKeys.CONFIG_NEMID_DOCURL));
	}

	private static void updateWCattributes(String sessionKey, String attrName, String wcURL, String tid, HttpServletRequest request) throws StatusCodeException {
		String sessionURL = DAOUtil.getSessionDataByKey(tid, sessionKey);
		if (!StringUtils.isNullorEmpty(sessionURL)) {
			request.setAttribute(attrName, sessionURL);
			logger.debug("Merchant override [" + attrName + "=" + sessionURL + "]");
		} else if (!StringUtils.isNullorEmpty(wcURL)) {
			request.setAttribute(attrName, wcURL);
			logger.debug("InsertOrder webcontext [" + attrName + "=" + wcURL + "]");
		}
	}

	private static SignerStatusTable[] getStatusTable(String mid, String sref, SigningProcess sp) throws StatusCodeException {
		try {
			GetStatusTableRequest gstq = new GetStatusTableRequest();
			gstq.setSignProcessID("" + sp.getSignprocessId());
			gstq.setOrderID(DAOUtil.getOrderID(sp));
			gstq.setMerchantID(mid);
			gstq.setTransId(sref);

			Requestor requestor = new Requestor(getConfigProperty(ConfigKeys.CONFIG_TRUSTENGINE_URL), 10000L);
			TEMessage temessage = requestor.sendRequest(gstq);

			if (temessage instanceof ErrorResponse) {
				ErrorResponse ers = (ErrorResponse) temessage;
				throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, "Unable to get status table [SREF=" + sref + "][ErrorCode="
						+ ers.getErrorCode() + "][ErrorText=" + ers.getErrorText() + "]");
			}

			GetStatusTableResponse gsts = (GetStatusTableResponse) temessage;
			return gsts.getStatusTable();
		} catch (Exception t) {
			EventLogger.dumpStack(t, logger);
			throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, "Unable to finalize SigningProcess [SREF=" + sref + "] Reason"
					+ t.getMessage());
		}
	}

}
