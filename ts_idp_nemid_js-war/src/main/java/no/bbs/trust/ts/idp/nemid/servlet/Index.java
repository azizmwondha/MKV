/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.servlet;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.i18n.LangSupport;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;
import no.bbs.trust.ts.idp.nemid.tag.ChallengeGenerator;
import no.bbs.trust.ts.idp.nemid.tag.OcesJsonParameterGenerator;
import no.bbs.trust.ts.idp.nemid.tag.Signer;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContext;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.trust.ts2.idp.common.statustable.StatusTableRetriever;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.constant.SessionKey;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SessionDataDAO;
import no.bbs.tt.trustsign.trustsignDAL.tx.TransactionHelper;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObject;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.WebContext;

import org.openoces.ooapi.utils.Base64Handler;
import org.springframework.transaction.TransactionStatus;

/**
 */
public class Index extends BaseServlet {

	private static final long serialVersionUID = 1L;

	private static final String COMPONENT_NAME = "NemIDJS";
	private static final String[] SESSION_DATA_KEYS = new String[] { ConfigKeys.SESSIONKEY_SPID, ConfigKeys.SESSIONKEY_MID, ConfigKeys.SESSIONKEY_LOCALE,
			ConfigKeys.SESSIONKEY_TZO, ConfigKeys.SESSIONKEY_NEMID_CLIENTMODE };

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
			Map<String, String> sessionDatas = DAOUtil.getSessionDataKeysAndValues(sref, SESSION_DATA_KEYS);

			String clientMode = getClientMode(request, sessionDatas);
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
			setSigningDocument(clientGenerator, signingProcess);
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
				request.setAttribute("statustable", StatusTableRetriever.getStatusTable(mid, signingProcess)); //invoking method from common IDP
			} catch (Exception exp) {
				logger.warn("Error in retrieving status table for SREF=" + sref + ", message=" + exp.toString());
			}

			EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_GENERATE_CLIENT_TAG, start);
			commit = true;
			return new ReturnCode(Dispatch.INCLUDE, "/index.jsp");
		} finally {
			transactionHelper.commitOrRollback(tx, commit);
		}
	}

	static String getClientMode(HttpServletRequest request, Map<String, String> sessionDatas) {
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

	static OcesJsonParameterGenerator createClientGenerator(String mid) {
		MerchantContext merchantContext = MerchantContextCache.getMerchantContext(mid);
		Map<String, String> idpConfig = merchantContext.getIdpConfig();

		String keystorePath = idpConfig.get(PKIConfigKeys.KEYSTORE);
		String keystorePwd = idpConfig.get(PKIConfigKeys.KEYSTORE_PASSWORD);
		String keyAlias = idpConfig.get(PKIConfigKeys.MERCHANT_ALIAS);
		String keyAliasPwd = idpConfig.get(PKIConfigKeys.MERCHANT_ALIAS_PASSWORD);

		Signer signer = new Signer(keystorePath, keystorePwd, keyAlias, keyAliasPwd);
		return new OcesJsonParameterGenerator(signer);
	}

	static void setSigningDocument(OcesJsonParameterGenerator clientGenerator, SigningProcess signingProcess) throws StatusCodeException {
		SignObjectData signObjectData = DAOUtil.getSignObjectData(signingProcess);
		SignObject signObject = DAOUtil.getSignObject(signObjectData.getSignerObjectId());
		String docType = signObjectData.getElementType();
		String docTitle = signObject.getTitle();
		String docDescription = signObject.getDescription();
		logger.debug("Doc type: " + docType + ", title: " + docTitle + ", description: " + docDescription);

		String signText = signObjectData.getObjectB64();
		if ("txt,text,text/plain".contains(docType.toLowerCase())) {
			docType = "text";
		} else {
			docType = "pdf";
		}
		clientGenerator.setSignText(signText, docType);
	}

	private static void setupWebContext(String tid, SigningProcess sp, HttpServletRequest request) throws StatusCodeException {
		WebContext wc = DAOUtil.getWebContext(sp.getWebcontextid());

		// Web context and merchant URLs
		updateWCattributes(ConfigKeys.SESSIONKEY_STYLE, "styleurl", wc.getStyleUrl(), tid, request);
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
}
