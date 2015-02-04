package no.bbs.trust.ts.idp.nemid.utils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;
import no.bbs.tt.trustsign.trustsignDAL.constant.StatusTypes;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.PkiCertificatePolicyDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SessionDataDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SignObjectDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SignObjectDataDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SignatureDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SignerAcceptPkiDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SignerIdDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.SigningProcessDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.StepDAO;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.WebContextDAO;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.PkiPolicy;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SessionData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObject;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignObjectData;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Signature;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignerAcceptPki;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignerId;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.Step;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.WebContext;

public class DAOUtil {

	public static String getOrderID(SigningProcess sp) throws StatusCodeException {
		try {
			StepDAO stdao = new StepDAO();
			Step st = stdao.getByStepId(null, sp.getStepId());

			if (null == st) {
				throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "No Step for SREF in DB");
			}

			return st.getOrderId();
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve OrderID for SREF: " + se.getMessage());
		}
	}

	public static SigningProcess getSigningProcess(int spid) throws StatusCodeException {
		try {
			SigningProcessDAO spdao = new SigningProcessDAO();
			SigningProcess sp = spdao.getBySignprocessid(null, spid);

			if (null == sp) {
				throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "No SigningProcess for SREF in DB");
			}

			return sp;
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot SigningProcess for SREF: " + se.getMessage());
		}
	}

	public static void updateSigningProcessStatus(SigningProcess signingProcess) throws StatusCodeException {
		try {
			SigningProcessDAO spdao = new SigningProcessDAO();
			spdao.updateStatus(null, signingProcess.getSignprocessId(), StatusTypes.COMPLETE_ID);
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot update SigningProcess status for SPID=" + se.getMessage());
		}
	}

	public static void storeSignature(int spid, Date signTime, String signerID, String signerCN, String signerOIDS, String dsig, String ocsp) throws StatusCodeException {
		try {
			SignatureDAO signdao = new SignatureDAO();
			Signature signInfo = signdao.getInfoBySignprocid(null, spid);

			if (null == signInfo) {
				signdao.insertXMLDsig(null, 0, spid, signerCN, signerOIDS, dsig, ocsp, PKIIDMap.DKNEMIDJS_ID, signTime, signerID);
			} else {
				throw new StatusCodeException(NemIDActionEvent.SIGNING_ALREADY_DONE, "Signature already exists for this SignProcess [SignProcessID=" + spid + "][signerCN="
						+ signerCN + "]");
			}
		} catch (SQLException se) {
			EventLogger.dumpStack(se);
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "" + se.getMessage());
		}
	}

	public static void validateSessionStep(String sref, int[] expectedSteps) throws StatusCodeException {
		try {
			SessionDataDAO sessionDao = new SessionDataDAO();
			SessionData sd = sessionDao.getBySrefAndKey(sref, ConfigKeys.SESSIONKEY_STEP);
			if (null != sd) {
				String sessionStep = sd.getVal();
				StringBuilder steps = new StringBuilder();

				for (int expectedStep : expectedSteps) {
					if (Integer.parseInt(sessionStep) == expectedStep) {
						return;
					}
					steps.append(expectedStep).append(",");
				}
				throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Session STEP is out of sequence [ExpectedStep=" + steps.toString() + "][FoundStep="
						+ sd.getVal() + "]");
			}
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "STEP session data not found in DB");
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot verify session step: " + se.getMessage());
		}
	}

	public static SignerId getSignerID(int signerID) throws StatusCodeException {
		try {
			SignerIdDAO sidao = new SignerIdDAO();
			return sidao.getBySignerIdAndPKIId(null, signerID, PKIIDMap.DKNEMIDJS_ID);
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot get SignerID: " + se.getMessage());
		}
	}

	public static void validateSignerID(SigningProcess sp, String id, String idType) throws StatusCodeException {
		if (!StringUtils.isNullorEmpty(id)) {
			SignerId si = getSignerID(sp.getSignerId());

			if (null != si) {
				if (si.getIdKey().equalsIgnoreCase(idType)) {
					String orderIDValue = ("" + si.getIdValue()).trim();

					if (orderIDValue.equalsIgnoreCase(id)) {
						return;
					}
					if (idType.equalsIgnoreCase("RID")) {
						if (id.indexOf(orderIDValue + "-") == 0) {
							// SignerID in in the form:
							// id -> CVR:1234-RID:9876
							// db -> CVR:1234
							return;
						}
					}
					// We only come this far when the IDs do not match (insufficient rights)
				} else {
					return;
				}
			} else {
				return;
			}
			NemIDActionEvent statusEvent = NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH;
			if (idType.equalsIgnoreCase("ssn"))
				statusEvent = NemIDActionEvent.STATUS_VERIFY_CPRMISMATCH;
			else if (idType.equalsIgnoreCase("pid"))
				statusEvent = NemIDActionEvent.STATUS_VERIFY_PIDMISMATCH;
			else if (idType.equalsIgnoreCase("rid"))
				statusEvent = NemIDActionEvent.STATUS_VERIFY_RIDMISMATCH;
			throw new StatusCodeException(statusEvent, "Document signer " + idType + " does not match Baseorder requirement. Permission denied");
		}
	}

	public static void validateSignerOIDs(SigningProcess sp, String oid) throws StatusCodeException {
		String certTypes = DAOUtil.getCertificateTypes(sp.getSignerId());

		if (null != certTypes && (!certTypes.equalsIgnoreCase("all"))) {
			String typeOIDs = getCertificateTypeOIDs(certTypes);
			if (("," + typeOIDs + ",").indexOf("," + oid + ",") > -1) {
				return;
			}
			throw new StatusCodeException(NemIDActionEvent.STATUS_VERIFY_CERT_TYPE_FAILED, "Signer certificate policyOID does not match requested type [SignerOID=" + oid
					+ "][ExpectedOIDs=" + typeOIDs + "][CertTypes=" + certTypes + "]. Permission denied");
		}
	}

	public static String getSessionDataByKey(String sref, String key) throws StatusCodeException {
		try {
			SessionDataDAO sessionDao = new SessionDataDAO();
			SessionData sd = sessionDao.getBySrefAndKey(sref, key);

			if (null != sd) {
				return sd.getVal();
			}
			return "";
		} catch (SQLException se) {
			EventLogger.dumpStack(se);
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to read session data");
		}
	}

	public static Map<String, String> getSessionDataKeysAndValues(String sref, String[] keys) throws StatusCodeException {
		try {
			SessionDataDAO sessionDao = new SessionDataDAO();
			Collection<SessionData> sessionDatas = sessionDao.getBySrefAndKeys(null, sref, keys);

			Map<String, String> sessionDataKeysAndValues = new HashMap<String, String>();
			for (SessionData sessionData : sessionDatas) {
				sessionDataKeysAndValues.put(sessionData.getKey(), sessionData.getVal());
			}
			return sessionDataKeysAndValues;
		} catch (SQLException se) {
			EventLogger.dumpStack(se);
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to read session data");
		}
	}

	public static void updateSessionDataByKey(String sref, String key, String value) throws StatusCodeException {
		try {
			SessionDataDAO sessionDao = new SessionDataDAO();
			SessionData sd = sessionDao.getBySrefAndKey(sref, key);

			if (null != sd) {
				sessionDao.insert(sref, key, value);
			} else {
				sessionDao.insert(sref, key, value);
			}
		} catch (SQLException se) {
			EventLogger.dumpStack(se);
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to update session data");
		}
	}

	public static SignObjectData getSignObjectData(SigningProcess sp) throws StatusCodeException {
		try {
			SignObjectDataDAO sodao = new SignObjectDataDAO();
			SignObjectData[] sods = sodao.getById(null, sp.getSignObjId());

			if ((null != sods) && (sods.length > 0)) {
				return sods[0];
			}
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "No document data associated with sign process in DB");
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve document for sign process: " + se.getMessage());
		}
	}

	public static SignObject getSignObject(int objectID) throws StatusCodeException {
		try {
			SignObjectDAO sodao = new SignObjectDAO();
			SignObject so = sodao.getById(null, objectID);

			if (null != so) {
				return so;
			}
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "No document description associated with SREF in DB");
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve document for SREF: " + se.getMessage());
		}
	}

	public static String getCertificateTypes(int signerID) throws StatusCodeException {
		try {
			SignerAcceptPkiDAO sapdao = new SignerAcceptPkiDAO();
			List<SignerAcceptPki> saps = sapdao.getBySignerid(null, signerID);

			if (null == saps) {
				return null;
			}

			StringBuilder certTypes = new StringBuilder();
			for (SignerAcceptPki sap : saps) {
				if (sap.getPkiId() == PKIIDMap.DKNEMIDJS_ID) {
					if (certTypes.length() > 0) {
						certTypes.append(",");
					}
					certTypes.append(sap.getCertType());
				}
			}
			return certTypes.toString();

		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve cert types for SREF: " + se.getMessage());
		}
	}

	private static String getCertificateTypeOIDs(String certType) throws StatusCodeException {
		try {
			PkiCertificatePolicyDAO pcpdao = new PkiCertificatePolicyDAO();
			List<PkiPolicy> pcps = pcpdao.getByPkiid(null, PKIIDMap.DKNEMIDJS_ID);

			if (null != pcps) {
				StringBuilder certOIDs = new StringBuilder();
				for (Object pcpo : pcps) {
					PkiPolicy pcp = (PkiPolicy) pcpo;

					if (pcp.getCertType().equalsIgnoreCase(certType)) {
						if (certOIDs.length() > 0) {
							certOIDs.append(",");
						}
						certOIDs.append(pcp.getPolicyOid());
					}
				}
				return certOIDs.toString();
			}

			return null;
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve OIDs for [CertType=" + certType + "]: " + se.getMessage());
		}
	}

	public static WebContext getWebContext(int webContextId) throws StatusCodeException {
		try {
			WebContextDAO wcdao = new WebContextDAO();
			WebContext wc = wcdao.getById(null, webContextId);

			if (null != wc) {
				return wc;
			}

			return null;
		} catch (SQLException se) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Cannot retrieve web contect for SREF: " + se.getMessage());
		}
	}

}
