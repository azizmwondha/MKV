package no.bbs.trust.ts.idp.nemid.verify;

public class VerifyClientSignatureResponseData extends AbstractData {
	private String signature = null;
	private String ocsp = null;
	private String signerPID = null;
	private String signerCPR = null;
	private String signerRID = null;

	public void setB64signature(String signature) {
		this.signature = signature;
	}

	public String getB64signature() {
		return signature;
	}

	public void setB64ocsp(String ocsp) {
		this.ocsp = ocsp;
	}

	public String getB64ocsp() {
		return ocsp;
	}

	public String getSignerPID() {
		return signerPID;
	}

	public void setSignerPID(String signerPID) {
		this.signerPID = signerPID;
	}

	public String getSignerCPR() {
		return signerCPR;
	}

	public void setSignerCPR(String signerCPR) {
		this.signerCPR = signerCPR;
	}

	public String getSignerRID() {
		return signerRID;
	}

	public void setSignerRID(String signerRID) {
		this.signerRID = signerRID;
	}
}
