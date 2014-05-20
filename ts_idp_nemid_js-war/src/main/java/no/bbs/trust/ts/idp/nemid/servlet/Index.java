/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.servlet;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import no.bbs.trust.ts.idp.nemid.tag.AppletElementGenerator;
import no.bbs.trust.ts.idp.nemid.tag.ChallengeGenerator;
import no.bbs.trust.ts.idp.nemid.tag.OcesJSONParameterGenerator;
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
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.WebContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import eu.nets.no.vas.esign.sdosigner.types.KeyCredentials;

/**
 *
 * @author azm
 */
public class Index extends BaseServlet {

	public static final String COMPONENT_NAME = "NemIDJS";

	@Override
	protected ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		logger.info("NemID JS: Get NemID client activation tag");
		String sref = "" + request.getParameter(ConfigKeys.PARAM_SREF);

		DAOUtil.validateSessionStep(sref, new int[] { 2, 4, 5, 6 });

		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		SigningProcess sp = DAOUtil.getSigningProcess(spid);

		if (!sref.equalsIgnoreCase(sp.getSignProcessRef())) {
			logger.info("SignProcess reference updated since session started (OneTimeUrl) [PreviousSREF=" + sref + "][NewSREF=" + sp.getSignProcessRef() + "]");
		}

		DAOUtil.updateSessionDataByKey(sref, ConfigKeys.SESSIONKEY_STEP, "5");

		String mid = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_MID);
		String locale = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_LOCALE);
		locale = (locale.trim().length() > 0) ? locale : LangSupport.getDefaultLanguage();
		request.setAttribute("locale", locale);
		String languageCode = null;
		try {
			languageCode = new SessionDataDAO().getBySrefAndKey(sref, SessionKey.LOCALE).getVal();
		} catch (SQLException e) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, e, COMPONENT_NAME, sref);
		}

		String tag = OcesJSONParameterGenerator.generateClientTag(mid, Config.INSTANCE.getProperty("nemid.client.mode.standard"), languageCode);
		logger.debug("NemID JS tag: " + tag);
		request.setAttribute("clienttag", tag);

		setupWebContext(sref, sp, request);
		request.setAttribute("sref", sref);

		// Parse signer deadline
		long endtime = sp.getDeadline().getTime();

		SimpleDateFormat formatter = new SimpleDateFormat(LangSupport.getUserText("format.mediumdate", locale));
		String tzos = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_TZO);
		tzos = (tzos.trim().length() > 0) ? tzos : "0";
		long tzo = Long.parseLong(tzos);

		request.setAttribute("signstatus.deadline", formatter.format(new Date(endtime + tzo)));
		request.setAttribute("tzo", tzos);

		try {
			request.setAttribute("statustable", getStatusTable(mid, sref, sp));
		} catch (StatusCodeException sce) {
			logger.info("No status table available for SREF");
		}
		EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_GENERATE_CLIENT_TAG, start);
		return new ReturnCode(Dispatch.INCLUDE, "/index.jsp");
	}

	private AppletElementGenerator createGenerator(String midf) throws StatusCodeException {
		try {
			KeyCredentials credentials = DAOUtil.getMerchantCredentials(midf);

			String keystore = credentials.getKeystorepath();
			String keystorePasswd = credentials.getKeystorepass();
			String alias = credentials.getKeyalias();
			String aliasPasswd = credentials.getKeyaliaspass();

			Signer appletSigner = null;
			try {
				appletSigner = new Signer(keystore, keystorePasswd, alias, aliasPasswd);
			} catch (Throwable t) {
				logger.warn("Cannot create AppletSigner. Check merchant PKI config (maybe a password and/or alias mismatch)");
				throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_OPERATION_FAILED, "Cannot create applet signer for Merchant[" + midf + "] "
						+ t.getMessage());
			}
			String challenge = ChallengeGenerator.generateChallenge();

			AppletElementGenerator generator = new AppletElementGenerator(appletSigner);
			generator.setServerUrlPrefix(Config.INSTANCE.getProperty(ConfigKeys.APPLET_URL_PREFIX));
			generator.setChallenge(challenge);
			if (logger.getLevel() != Level.INFO) {
				generator.setLogLevel("debug"); // INFO/DEBUG/ERROR
			} else {
				generator.setLogLevel("info"); // INFO/DEBUG/ERROR
			}
			generator.addSignedParameter("always_embedded", "true");

			return generator;
		} catch (SQLException ex) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to obtain key credentials for Merchant[" + midf + "] "
					+ ex.getMessage());
		}
	}

	private String generateAppletTag(String midf, String sref, SigningProcess sp) throws StatusCodeException {
		AppletElementGenerator appletGenerator = createGenerator(midf);

		SignObjectData signObject = DAOUtil.getSignObjectData(sp);
		String doctype = signObject.getElementType();
		String docTitle = DAOUtil.getSignObject(signObject.getSignerObjectId()).getTitle();
		String docDescr = DAOUtil.getSignObject(signObject.getSignerObjectId()).getDescription();

		if ("txt,text,text/plain".indexOf(doctype.toLowerCase()) > -1) {
			doctype = "text/plain";
			String signText = utf8b642iso88591String(signObject.getObjectB64());
			logger.debug("Document signText: " + signText);
			appletGenerator.setSignText(signText, doctype);
		} else {
			Attachment attachment = new Attachment();
			attachment.setTitle(docTitle);
			attachment.setMimeType(doctype);

			byte[] documentBytes = Base64.decode(signObject.getObjectB64());
			try {
				String dochash = new String(Base64.encode(HashUtil.hash(documentBytes, no.bbs.trust.ts.idp.nemid.contants.Constants.DIGEST_SHA2)));
				attachment.setB64HashValue(dochash);
				attachment.setB64HashAlgo(no.bbs.trust.ts.idp.nemid.contants.Constants.DIGEST_SHA2_SHORTNAME);
			} catch (NoSuchAlgorithmException ex) {
				Logger.getLogger(Index.class.getName()).error(ex);
			} catch (NoSuchProviderException ex) {
				Logger.getLogger(Index.class.getName()).error(ex);
			}

			String docUrl = getConfigProperty(ConfigKeys.CONFIG_NEMID_DOCURL) + "?" + ConfigKeys.PARAM_SREF + "=" + sref;
			attachment.setPath(docUrl);
			attachment.setSize(documentBytes.length);

			logger.trace("Attachment: " + attachment.toXML());

			appletGenerator.setSignText(docDescr, "text/plain");
			appletGenerator.setPDF(attachment);
		}

		String certType = DAOUtil.getCertificateTypes(sp.getSignerId());
		String subjectDnFilter = null;
		logger.debug("CertType: " + certType);
		if (Constants.CERTTYPE_EMPLOYEE.equals(certType)) {
			subjectDnFilter = Constants.SUBJECT_DN_FILTER_RID;
		} else if (Constants.CERTTYPE_PERSONAL.equals(certType)) {
			subjectDnFilter = Constants.SUBJECT_DN_FILTER_PID;
		}
		logger.debug("SubjectDNFilter: " + subjectDnFilter);
		return appletGenerator.generateSignAppletElement(sref, subjectDnFilter, Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_TAG_VERIFYURL));
	}

	private void setupWebContext(String tid, SigningProcess sp, HttpServletRequest request) throws StatusCodeException {
		WebContext wc = DAOUtil.getWebContext(sp.getWebcontextid());

		// Web context and merchant URLs
		//		updateWCattributes(ConfigKeys.SESSIONKEY_STATUS, "statusurl", wc.getErrorUrlBase(), tid, request);
		//		updateWCattributes(ConfigKeys.SESSIONKEY_CANCEL, "cancelurl", wc.getAbortUrl(), tid, request);
		updateWCattributes(ConfigKeys.SESSIONKEY_STYLE, "styleurl", wc.getStyleUrl(), tid, request);

		// SE_BID application URLs
		request.setAttribute("verifyurl", getConfigProperty(ConfigKeys.CONFIG_NEMID_TAG_VERIFYURL));
		request.setAttribute("docurl", getConfigProperty(ConfigKeys.CONFIG_NEMID_DOCURL));
	}

	private void updateWCattributes(String sessionKey, String attrName, String wcURL, String tid, HttpServletRequest request) throws StatusCodeException {
		String sessionURL = DAOUtil.getSessionDataByKey(tid, sessionKey);
		if (!StringUtils.isNullorEmpty(sessionURL)) {
			request.setAttribute(attrName, sessionURL);
			logger.debug("Merchant override [" + attrName + "=" + sessionURL + "]");
		} else if (!StringUtils.isNullorEmpty(wcURL)) {
			request.setAttribute(attrName, wcURL);
			logger.debug("InsertOrder webcontext [" + attrName + "=" + wcURL + "]");
		}
	}

	private SignerStatusTable[] getStatusTable(String mid, String sref, SigningProcess sp) throws StatusCodeException {
		TEMessage temessage = null;
		try {
			GetStatusTableRequest gstq = new GetStatusTableRequest();
			gstq.setSignProcessID("" + sp.getSignprocessId());
			gstq.setOrderID(DAOUtil.getOrderID(sp));
			gstq.setMerchantID(mid);
			gstq.setTransId(sref);

			Requestor requestor = new Requestor(getConfigProperty(ConfigKeys.CONFIG_TRUSTENGINE_URL), 10000L);
			temessage = requestor.sendRequest(gstq);

			if (temessage instanceof ErrorResponse) {
				ErrorResponse ers = (ErrorResponse) temessage;
				throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, "Unable to get status table [SREF=" + sref + "][ErrorCode="
						+ ers.getErrorCode() + "][ErrorText=" + ers.getErrorText() + "]");
			}

			GetStatusTableResponse gsts = (GetStatusTableResponse) temessage;
			return gsts.getStatusTable();
		} catch (Throwable t) {
			EventLogger.dumpStack(t, logger);
			throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, "Unable to finalize SigningProcess [SREF=" + sref + "] Reason"
					+ t.getMessage());
		}
	}

	private String utf8b642iso88591String(String b64in) throws StatusCodeException {
		try {
			byte[] utfbytes = Base64.decode(b64in);
			b64in = null;
			byte[] isobytes = new String(utfbytes, "UTF-8").getBytes("ISO-8859-1");
			return new String(isobytes, "ISO-8859-1");
		} catch (UnsupportedEncodingException uex) {
			EventLogger.dumpStack(uex, logger);
			throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, uex.getMessage());
		}
	}

	private boolean runDetectorApplet(String sref) {
		SessionDataDAO sdd = new SessionDataDAO();
		try {
			String jre = sdd.getBySrefAndKey(sref, "JAVAVERSION").getVal();
			if (jre.equals("0")) {
				return true;
			} else {
				logger.info("User has [javaversion=" + jre + "]");
			}
		} catch (SQLException e) {
			EventLogger.dumpStack(e);
			logger.error("Error accessing db: " + e);
		}
		return false;
	}

	@Override
	public void doInit() throws ServletException {
	}

}
