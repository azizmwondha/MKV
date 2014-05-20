package no.bbs.trust.ts.idp.nemid.error;

import no.bbs.trust.common.config.Config;

import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;

public class NemIDErrorMapper
{
/*
nemid.codegroup.usercancel=can001,can002,app005
nemid.codegroup.opercancel=app005
nemid.codegroup.badprotocol=app003,app004,srv001,srv002,srv003
nemid.codegroup.uidrevoked=lock001,lock002,lock003
nemid.codegroup.uidinvalid=oces001
nemid.codegroup.uidexpired=
nemid.codegroup.authfailed=app001,auth001,auth002,auth003,auth004,auth005,auth006,auth007.auth008
 */
	
	public static ErrorCodes getErrorCodeFromNemIDCode(String nemidCode)
	{
		String userCancel = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_USERCANCEL);
		String operCancel = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_OPERCANCEL);
		String badProtocol = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_BADPROTOCOL);
		String uidRevoked = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDREVOKED);
		String uidInvalid = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDINVALID);
		String uidExpired = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_UIDEXPIRED);
		String authFailed = Config.INSTANCE.getProperty(ConfigKeys.NEMID_CODEGROUP_AUTHFAILED);
		
		if (userCancel.contains(nemidCode))
		{
			return ErrorCodes.USERCANCEL;
		}
		if (operCancel.contains(nemidCode))
		{
			return ErrorCodes.OPERCANCEL;
		}
		if (badProtocol.contains(nemidCode))
		{
			return ErrorCodes.BADPROTOCOL;
		}
		if (uidRevoked.contains(nemidCode))
		{
			return ErrorCodes.UIDREVOKED;
		}
		if (uidInvalid.contains(nemidCode))
		{
			return ErrorCodes.UIDINVALID;
		}
		if (uidExpired.contains(nemidCode))
		{
			return ErrorCodes.UIDEXPIRED;
		}
		if (authFailed.contains(nemidCode))
		{
			return ErrorCodes.AUTHFAILED;
		}

		return null;
	}
}
