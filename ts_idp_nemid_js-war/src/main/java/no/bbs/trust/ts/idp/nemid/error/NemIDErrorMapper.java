package no.bbs.trust.ts.idp.nemid.error;

import no.bbs.trust.common.config.Config;

import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;

public class NemIDErrorMapper {

	/*
	nemid.codegroup.usercancel=CAN001,CAN002
	nemid.codegroup.opercancel=
	nemid.codegroup.badprotocol=APP001,APP003,APP004,APP007,APP008,SRV001,SRV002,SRV003,SRV004,SRV005,SRV006,SRV007,SRV009
	nemid.codegroup.uidrevoked=LOCK001,LOCK002,LOCK003
	nemid.codegroup.uidinvalid=OCES001,OCES002,OCES003,OCES004,OCES005,OCES006
	nemid.codegroup.uidexpired=
	nemid.codegroup.authfailed=AUTH001,AUTH002,AUTH003,AUTH004,AUTH005,AUTH006,AUTH007,AUTH008,AUTH009,AUTH010,AUTH011,AUTH012
	nemid.codegroup.docinvalid=APP002
	*/

	public static ErrorCodes getErrorCodeFromNemIDCode(String nemidCode) {
		String userCancel = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_USERCANCEL);
		String operCancel = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_OPERCANCEL);
		String badProtocol = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_BADPROTOCOL);
		String uidRevoked = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDREVOKED);
		String uidInvalid = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDINVALID);
		String uidExpired = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDEXPIRED);
		String authFailed = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_AUTHFAILED);

		if (userCancel.contains(nemidCode)) {
			return ErrorCodes.USERCANCEL;
		}
		if (operCancel.contains(nemidCode)) {
			return ErrorCodes.OPERCANCEL;
		}
		if (badProtocol.contains(nemidCode)) {
			return ErrorCodes.BADPROTOCOL;
		}
		if (uidRevoked.contains(nemidCode)) {
			return ErrorCodes.UIDREVOKED;
		}
		if (uidInvalid.contains(nemidCode)) {
			return ErrorCodes.UIDINVALID;
		}
		if (uidExpired.contains(nemidCode)) {
			return ErrorCodes.UIDEXPIRED;
		}
		if (authFailed.contains(nemidCode)) {
			return ErrorCodes.AUTHFAILED;
		}

		return null;
	}

}
