/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package no.bbs.trust.ts.idp.nemid.event;

import no.bbs.trust.common.basics.events.PerformanceEvent;

/**
 *
 * @author azm
 */
public enum NemIDPerformanceEvent implements PerformanceEvent {

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
	DK_NEMID_HANDLE_REQUEST("33ED0"),
	DK_NEMID_UNUSED("33ED1"),
	DK_NEMID_GENERATE_CLIENT_TAG("33ED2"),
	DK_NEMID_GET_DOCUMENT("33ED3"),
	DK_NEMID_VERIFY_SIGNATURE("33ED4"),
	DK_NEMID_PID_MATCH("33ED5"),
	DK_NEMID_OCSP_LOOKUP("33ED6"),
	DK_NEMID_CREATE_SDO("33ED7"),
	DK_NEMID_SEAL_SDO("33ED8"),
	DK_NEMID_RID_MATCH("33ED6");
	//@formatter:on

	private String eventcode;

	private NemIDPerformanceEvent(String theEventCode) {
		eventcode = theEventCode;
	}

	@Override
	public String getCode() {
		return eventcode;
	}

}
