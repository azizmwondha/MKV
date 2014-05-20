package no.bbs.trust.ts.idp.nemid.verify;

public class AbstractData {
	private String mid;
	private String transref;
	private String time;
	public void setMid(String mid) {
		this.mid = mid;
	}
	public String getMid() {
		return mid;
	}
	public void setTransref(String transref) {
		this.transref = transref;
	}
	public String getTransref() {
		return transref;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getTime() {
		return time;
	}
}
