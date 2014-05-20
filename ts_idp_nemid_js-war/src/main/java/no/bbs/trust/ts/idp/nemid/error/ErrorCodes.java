package no.bbs.trust.ts.idp.nemid.error;

public enum ErrorCodes {
	/*
	 * nemid.codegroup.usercancel=can001,can002,app005
	nemid.codegroup.opercancel=app005
	nemid.codegroup.badprotocol=app003,app004,srv001,srv002,srv003
	nemid.codegroup.uidrevoked=lock001,lock002,lock003
	nemid.codegroup.uidinvalid=oces001
	nemid.codegroup.uidexpired=
	nemid.codegroup.authfailed=app001,auth001,auth002,auth003,auth004,auth005,auth006,auth007.auth008
	 */

	USERCANCEL,
	OPERCANCEL,
	BADPROTOCOL,
	UIDREVOKED,
	UIDINVALID,
	UIDEXPIRED,
	AUTHFAILED;
	
}
