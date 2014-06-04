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
	REQUEST_RECEIVED("33001"),
	RESPONSE_SENT("33002"),
	ACTION_IDP_DKNEMID_LIFECYCLE_STARTED("33003"),
	ACTION_IDP_DKNEMID_LIFECYCLE_STOPPED("33004"),
	
	STATUS_UNKNOWN_ERROR("33010"),
	STATUS_IDP_CACHE_ERROR("33011"),
	STATUS_DAL_SQL_ERROR("33012"),
	STATUS_DK_TRANSACTIONDATA_FOR_TID_GID("33013"),
	STATUS_INVALID_STEP("33014"),
	SIGNING_ALREADY_DONE("33015"),
	STATUS_AMQ_ERROR("33016"),
	STATUS_ENCODING_ERROR("33017"),

	STATUS_NEMID_SERVICE_LOAD_FAILED("33020"),
	STATUS_CREATE_SESSION_FAILED("33021"),
	STATUS_VERIFY_SIGN_FAILED("33022"),
	STATUS_VERIFY_CPRMISMATCH("33023"),
	STATUS_VERIFY_PIDMISMATCH("33024"),
	STATUS_VERIFY_RIDMISMATCH("33025"),
	STATUS_VERIFY_CERT_TYPE_FAILED("33026"),
	STATUS_USER_CANCEL("33027"),
	
	STATUS_VERIFY_INVALIDSTATUS("33029"),
	
	STATUS_UNEXPECTED_INTERNAL_ERROR("33030"),
	STATUS_IDP_OPERATION_FAILED("33031"),
	STATUS_IDP_CERT_PARSE_FAILED("33032"),
	STATUS_RID_LOOKUP_FAILED("33033"),
	STATUS_IDP_SETUP_FAIL("33034"),
	STATUS_PID_LOOKUP_FAILED("33035"),
	
	ACTION_DK_NEMID_RIDMATCH("33098"),
	ACTION_DK_NEMID_CPRMATCH("33099"),
	ACTION_DK_NEMID_SIGN_OK("3309A"),
	ACTION_DK_NEMID_GENERATE_TAG("3309B"),
	ACTION_LOAD_OCES_CONFIG("3309C"),
	ACTION_LOAD_IDP_CONFIG("3309D"),
	ACTION_LOAD_MERCHANT_CONTEXTS("3309E"),
	ACTION_IDP_DK_NEMID_LIFECYCLE("3309F");
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
