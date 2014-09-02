package no.bbs.trust.ts.idp.nemid.event;

import no.bbs.trust.common.basics.events.ActionEvent;

/**
 *
 * @author azm
 */
public enum NemIDActionEvent implements ActionEvent {

	/*
	 * Code range 0x33000 - 0x330FF Reserved for DK_NEMID
	 * --------------------------------------------------
	 * 0x33000 - 0x3309F Action events
	 * 0x330A0 - 0x330CF Measurement events
	 * 0x330D0 - 0x330FF Performance events
	 *
	 * NB: Event group ranges can overlap (i.e. there is no problem if an action event
	 * has the same code as a measurement event or a performance event).
	 */

	//@formatter:off
	REQUEST_RECEIVED("33E01"),
	RESPONSE_SENT("33E02"),
	ACTION_IDP_DKNEMID_LIFECYCLE_STARTED("33E03"),
	ACTION_IDP_DKNEMID_LIFECYCLE_STOPPED("33E04"),

	STATUS_UNKNOWN_ERROR("33E10"),
	STATUS_IDP_CACHE_ERROR("33E11"),
	STATUS_DAL_SQL_ERROR("33E12"),
	STATUS_DK_TRANSACTIONDATA_FOR_TID_GID("33E13"),
	STATUS_INVALID_STEP("33E14"),
	SIGNING_ALREADY_DONE("33E15"),
	STATUS_AMQ_ERROR("33E16"),
	STATUS_ENCODING_ERROR("33E17"),

	STATUS_NEMID_SERVICE_LOAD_FAILED("33E20"),
	STATUS_CREATE_SESSION_FAILED("33E21"),
	STATUS_VERIFY_SIGN_FAILED("33E22"),
	STATUS_VERIFY_CPRMISMATCH("33E23"),
	STATUS_VERIFY_PIDMISMATCH("33E24"),
	STATUS_VERIFY_RIDMISMATCH("33E25"),
	STATUS_VERIFY_CERT_TYPE_FAILED("33E26"),
	STATUS_USER_CANCEL("33E27"),

	STATUS_VERIFY_INVALIDSTATUS("33E29"),

	STATUS_UNEXPECTED_INTERNAL_ERROR("33E30"),
	STATUS_IDP_OPERATION_FAILED("33E31"),
	STATUS_IDP_CERT_PARSE_FAILED("33E32"),
	STATUS_RID_LOOKUP_FAILED("33E33"),
	STATUS_IDP_SETUP_FAIL("33E34"),
	STATUS_PID_LOOKUP_FAILED("33E35"),

	ACTION_DK_NEMID_RIDMATCH("33E98"),
	ACTION_DK_NEMID_CPRMATCH("33E99"),
	ACTION_DK_NEMID_SIGN_OK("33E9A"),
	ACTION_DK_NEMID_GENERATE_TAG("33E9B"),
	ACTION_LOAD_OCES_CONFIG("33E9C"),
	ACTION_LOAD_IDP_CONFIG("33E9D"),
	ACTION_LOAD_MERCHANT_CONTEXTS("33E9E"),
	ACTION_IDP_DK_NEMID_LIFECYCLE("33E9F");
	//@formatter:on

	private String eventcode;

	private NemIDActionEvent(String theEventCode) {
		eventcode = theEventCode;
	}

	@Override
	public String getCode() {
		return eventcode;
	}

}
