package no.bbs.trust.ts.idp.nemid.error;

public enum ErrorCodes {

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

	//@formatter:off
	USERCANCEL,
	OPERCANCEL,
	BADPROTOCOL,
	UIDREVOKED,
	UIDINVALID,
	UIDEXPIRED,
	AUTHFAILED,
	DOCINVALID;
	//@formatter:on

}
