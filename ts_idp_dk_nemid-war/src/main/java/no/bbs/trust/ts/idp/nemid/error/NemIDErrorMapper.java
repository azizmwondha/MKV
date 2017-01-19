package no.bbs.trust.ts.idp.nemid.error;

import java.util.HashMap;
import java.util.Map;

import no.bbs.trust.common.basics.events.ActionEvent;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;

public class NemIDErrorMapper {

	/*
	nemid.codegroup.usercancel=CAN001,CAN002,CAN004
	nemid.codegroup.opercancel=
	nemid.codegroup.badprotocol=APP001,APP003,APP004,APP007,APP008,APP009,APP010,SRV001,SRV002,SRV003,SRV004,SRV005,SRV006,SRV007,SRV009,SRV010
	nemid.codegroup.uidrevoked=LOCK001,LOCK002,LOCK003
	nemid.codegroup.uidinvalid=OCES001,OCES002,OCES003,OCES004,OCES005,OCES006
	nemid.codegroup.uidexpired=
	nemid.codegroup.authfailed=AUTH001,AUTH003,AUTH004,AUTH005,AUTH006,AUTH007,AUTH008,AUTH009,AUTH010,AUTH011,AUTH012,AUTH013,AUTH017
	nemid.codegroup.docinvalid=APP002
	nemid.codegroup.otptimeout=CAN003
	*/

	private static final String USER_CANCEL = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_USERCANCEL);
	private static final String OPER_CANCEL = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_OPERCANCEL);
	private static final String BAD_PROTOCOL = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_BADPROTOCOL);
	private static final String UID_REVOKED = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDREVOKED);
	private static final String UID_INVALID = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDINVALID);
	private static final String UID_EXPIRED = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDEXPIRED);
	private static final String AUTH_FAILED = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_AUTHFAILED);
	private static final String DOC_INVALID = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_DOCINVALID);
	private static final String OTP_TIMEOUT = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_OTPTIMEOUT);

	static final String USER_CANCEL_DESCRIPTION = "The user cancelled the operation";
	static final String OPER_CANCEL_DESCRIPTION = "The operator cancelled the operation";
	static final String BAD_PROTOCOL_DESCRIPTION = "Error in the protocol";
	static final String UID_REVOKED_DESCRIPTION = "The user ID is revoked";
	static final String UID_INVALID_DESCRIPTION = "The user ID is invalid";
	static final String UID_EXPIRED_DESCRIPTION = "The user ID has expired";
	static final String AUTH_FAILED_DESCRIPTION = "Authentication failed";
	static final String DOC_INVALID_DESCRIPTION = "The document is invalid";
	static final String OTP_TIMEOUT_DESCRIPTION = "The user has timed out due to inactivity";

	private static final Map<String, NemIDActionEvent> errorCodeToActionEventMap = new HashMap<String, NemIDActionEvent>();
	private static final Map<String, String> errorCodeDescriptionMap = new HashMap<String, String>();

	// Initialize the errorCodeToActionEventMap and errorCodeDescriptionMap
	static {
		for (String nemidErrorCode : USER_CANCEL.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_USER_CANCEL);
			errorCodeDescriptionMap.put(nemidErrorCode, USER_CANCEL_DESCRIPTION);
		}
		for (String nemidErrorCode : OPER_CANCEL.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_OPER_CANCEL);
			errorCodeDescriptionMap.put(nemidErrorCode, OPER_CANCEL_DESCRIPTION);
		}
		for (String nemidErrorCode : BAD_PROTOCOL.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_BAD_PROTOCOL);
			errorCodeDescriptionMap.put(nemidErrorCode, BAD_PROTOCOL_DESCRIPTION);
		}
		for (String nemidErrorCode : UID_REVOKED.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_UID_REVOKED);
			errorCodeDescriptionMap.put(nemidErrorCode, UID_REVOKED_DESCRIPTION);
		}
		for (String nemidErrorCode : UID_INVALID.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_UID_INVALID);
			errorCodeDescriptionMap.put(nemidErrorCode, UID_INVALID_DESCRIPTION);
		}
		for (String nemidErrorCode : UID_EXPIRED.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_UID_EXPIRED);
			errorCodeDescriptionMap.put(nemidErrorCode, UID_EXPIRED_DESCRIPTION);
		}
		for (String nemidErrorCode : AUTH_FAILED.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_AUTH_FAILED);
			errorCodeDescriptionMap.put(nemidErrorCode, AUTH_FAILED_DESCRIPTION);
		}
		for (String nemidErrorCode : DOC_INVALID.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_DOC_INVALID);
			errorCodeDescriptionMap.put(nemidErrorCode, DOC_INVALID_DESCRIPTION);
		}
		for (String nemidErrorCode : OTP_TIMEOUT.split(",")) {
			errorCodeToActionEventMap.put(nemidErrorCode, NemIDActionEvent.STATUS_OTP_TIMEOUT);
			errorCodeDescriptionMap.put(nemidErrorCode, OTP_TIMEOUT_DESCRIPTION);
		}
	}

	/**
	 * Gets the ActionEvent for the specified NemID error code.
	 * 
	 * @param nemidErrorCode the NemID error code to find the ActionEvent for
	 * @return the ActionEvent for the specified NemID error code
	 */
	public static ActionEvent getActionEvent(String nemidErrorCode) {
		ActionEvent actionEvent = errorCodeToActionEventMap.get(nemidErrorCode);
		return actionEvent != null ? actionEvent : NemIDActionEvent.STATUS_UNKNOWN_ERROR;
	}

	/**
	 * Gets the error code description for the specified NemID error code.
	 * 
	 * @param nemidErrorCode the NemID error code to find the description for
	 * @return the error code description for the specified NemID error code
	 */
	public static String getErrorCodeDescription(String nemidErrorCode) {
		String errorCodeDescription = errorCodeDescriptionMap.get(nemidErrorCode);
		return errorCodeDescription != null ? errorCodeDescription : "";
	}

}
