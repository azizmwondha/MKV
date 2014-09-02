package no.bbs.trust.ts.idp.nemid.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.CharsetType;
import no.bbs.trust.common.basics.types.ContentType;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author azm
 */
public class GetDocument extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		logger.info("Get document/download to user agent");
		String sref = request.getParameter(ConfigKeys.PARAM_SREF);

		DAOUtil.validateSessionStep(sref, new int[] { 5 });

		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		SigningProcess sp = DAOUtil.getSigningProcess(spid);

		if (!sref.equalsIgnoreCase(sp.getSignProcessRef())) {
			logger.info("SignProcess reference updated since session started (OneTimeUrl) [PreviousSREF=" + sref + "][NewSREF=" + sp.getSignProcessRef() + "]");
		}

		SignObjectData signObject = DAOUtil.getSignObjectData(sp);
		byte[] documentData = Base64.decode(signObject.getObjectB64());
		String docType = signObject.getElementType();

		logger.info("[DocumentType=" + docType + "][DocumentSize=" + documentData.length + "]");
		EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_GET_DOCUMENT, start);

		if ("txt,text,text/plain".contains(docType.toLowerCase())) {
			return new ReturnCode(documentData, ContentType.TEXT_PLAIN, CharsetType.UTF_8);
		}

		ReturnCode returnCode = new ReturnCode(documentData, ContentType.APPLICATION_PDF);
		returnCode.addHeader("Access-Control-Allow-Origin", Config.INSTANCE.getProperty("nemid.client.origin"));
		return returnCode;
	}
}
