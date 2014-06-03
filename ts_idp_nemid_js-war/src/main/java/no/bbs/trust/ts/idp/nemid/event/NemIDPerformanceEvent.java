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
	DK_NEMID_HANDLE_REQUEST("330D0"),
	DK_NEMID_UNUSED("330D1"),
	DK_NEMID_GENERATE_CLIENT_TAG("330D2"),
	DK_NEMID_GET_DOCUMENT("330D3"),
	DK_NEMID_VERIFY_SIGNATURE("330D4"),
	DK_NEMID_PID_MATCH("330D5"),
	DK_NEMID_OCSP_LOOKUP("330D6"),
	DK_NEMID_CREATE_SDO("330D7"),
	DK_NEMID_SEAL_SDO("330D8"),
	DK_NEMID_RID_MATCH("330D6");
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
