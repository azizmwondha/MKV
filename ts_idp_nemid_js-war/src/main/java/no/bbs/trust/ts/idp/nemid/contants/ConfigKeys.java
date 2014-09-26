package no.bbs.trust.ts.idp.nemid.contants;

public class ConfigKeys {

	public static final String PARAM_SREF = "sref";
	public static final String PARAM_NEMID_CLIENTMODE = "nemid_clientmode";

	public static final String SESSIONKEY_STEP = "STEP";
	public static final String SESSIONKEY_MID = "MERCHANTID";
	public static final String SESSIONKEY_SPID = "SPID";
	public static final String SESSIONKEY_PKIID = "SELECTEDPKIID";
	public static final String SESSIONKEY_LOCALE = "LOCALE";
	public static final String SESSIONKEY_CANCEL = "CANCEL";
	public static final String SESSIONKEY_STATUS = "STATUS";
	public static final String SESSIONKEY_STYLE = "STYLE";
	public static final String SESSIONKEY_TZO = "TZO";
	public static final String SESSIONKEY_CHALLENGE = "CHALLENGE";
	public static final String SESSIONKEY_NEMID_CLIENTMODE = "NEMID_CLIENTMODE";

	public static final String CONFIG_NEMID_DOCURL = "nemid.url.document";
	public static final String CONFIG_TRUSTENGINE_URL = "trustengine.url";

	public static final String CONFIG_NEMID_VERIFYURL = "nemid.url.verify";
	public static final String CONFIG_NEMID_STATUSURL = "nemid.url.status";
	public static final String CONFIG_NEMID_CANCELURL = "nemid.url.cancel";
	public static final String CONFIG_NEMID_RECEIPTURL = "nemid.url.receipt";

	public static final String CONFIG_PROXY_USER = "proxy.user";
	public static final String CONFIG_PROXY_PASS = "proxy.pass";
	public static final String CONFIG_PROXY_HOST = "proxy.host";
	public static final String CONFIG_PROXY_PORT = "proxy.port";

	public static final String CONFIG_SUPPORTED_LOCALES = "i18n.supportedlocales";
	public static final String CONFIG_HTML_CHARSET = "http.html.charset";

	public static final String CPR_TRUSTSTORE_PATH = "cprreg.truststore.file";
	public static final String CPR_TRUSTSTORE_TYPE = "cprreg.truststore.type";
	public static final String CPR_TRUSTSTORE_PASSWORD = "cprreg.truststore.password";

	public static final String RID_TRUSTSTORE_PATH = "ridreg.truststore.file";
	public static final String RID_TRUSTSTORE_TYPE = "ridreg.truststore.type";
	public static final String RID_TRUSTSTORE_PASSWORD = "ridreg.truststore.password";

	public static final String CPRREG_LOOKUP_URL = "cprreg.lookup.url";
	public static final String CPRREG_REQUESTISSUER = "cprreg.request.issuer";

	public static final String DANID_ENVIRONMENT = "danid.environment";

	public static final String APPLET_URL_PREFIX = "applet.url.prefix";

	public static final String CPR_LOOKUP_PROXYHOST = "cprreg.proxy.host";
	public static final String CPR_LOOKUP_PROXYPORT = "cprreg.proxy.port";
	public static final String CPR_LOOKUP_PROXYUSER = "cprreg.proxy.user";
	public static final String CPR_LOOKUP_PROXYPASS = "cprreg.proxy.password";

	public static final String NEMID_OIDS = "nemid.oids";

	public static final String NEMID_CODEGROUP_USERCANCEL = "nemid.codegroup.usercancel";
	public static final String NEMID_CODEGROUP_OPERCANCEL = "nemid.codegroup.opercancel";
	public static final String NEMID_CODEGROUP_BADPROTOCOL = "nemid.codegroup.badprotocol";
	public static final String NEMID_CODEGROUP_UIDREVOKED = "nemid.codegroup.uidrevoked";
	public static final String NEMID_CODEGROUP_UIDINVALID = "nemid.codegroup.uidinvalid";
	public static final String NEMID_CODEGROUP_UIDEXPIRED = "nemid.codegroup.uidexpired";
	public static final String NEMID_CODEGROUP_AUTHFAILED = "nemid.codegroup.authfailed";
	public static final String NEMID_CODEGROUP_DOCINVALID = "nemid.codegroup.docinvalid";

	public static final String RIDREG_LOOKUP_URL = "ridreg.lookup.url";

	// Activation tag keys.
	public static final String CONFIG_NEMID_TAG_SOURCE = "nemid.clienttag.source";
	public static final String CONFIG_NEMID_TAG_APPLETPATH = "nemid.clienttag.appletpath";
	public static final String CONFIG_NEMID_TAG_DIGEST = "paramsdigest";
	public static final String CONFIG_NEMID_TAG_SIGNATURE = "signeddigest";
	public static final String CONFIG_NEMID_TAG_SUBJECTDNFILTER = "subjectdnfilter";
	public static final String CONFIG_NEMID_TAG_CHALLENGE = "nemid.clienttag.challenge";

	public static final String CONFIG_NEMID_TAG_PARAMS_UNSIGNED = "nemid.clienttag.unsignedparams";
	public static final String CONFIG_NEMID_TAG_PARAMS_SIGNED = "nemid.clienttag.signedparams";

	public static final String CONFIG_NEMID_TAG_VERIFYURL = "nemid.clienttag.verifyurl";
	public static final String CONFIG_NEMID_TAG_STATUSURL = "nemid.clienttag.statusurl";

	public static final String CONFIG_NEMID_CLIENTMODE_STANDARD = "nemid.clientmode.standard";
	public static final String CONFIG_NEMID_CLIENTMODE_LIMITED = "nemid.clientmode.limited";
	public static final String CONFIG_NEMID_CLIENTFLOW_SIGNING = "nemid.clientflow.signing";
	public static final String CONFIG_NEMID_CLIENT_LAUNCHER = "nemid.client.launcher";
	public static final String CONFIG_NEMID_CLIENT_ORIGIN = "nemid.client.origin";
	public static final String CONFIG_NEMID_CLIENT_SP_ORIGIN = "nemid.client.sp.origin";
	public static final String CONFIG_NEMID_CLIENT_URL = "nemid.client.url";

	public static final String CONFIG_NEMID_CLIENTTAG_DIV = "nemid.clienttag.div";
	public static final String CONFIG_NEMID_CLIENTTAG_PARAMETERS = "nemid.clienttag.parameters";
	public static final String CONFIG_NEMID_CLIENTTAG_IFRAME = "nemid.clienttag.iframe";
	public static final String CONFIG_NEMID_CLIENTTAG_POSTBACKFORM = "nemid.clienttag.postbackform";
	public static final String CONFIG_NEMID_CLIENTTAG_SCRIPT = "nemid.clienttag.script";

}
