package no.bbs.trust.ts.idp.nemid.verify;

public class VerifyClientSignatureData extends AbstractData {
	private String result = null;
	private String signature = null;
	private String challenge = null;
	private String b64Document = null;
	private String documentType = null;
	private String signerPID = null;
	private String signerCPR = null;
	private String signerRID = null;
	private String certType = null;
	
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

	public void setResult(String result) {
		this.result = result;
	}

	public String getResult() {
		return result;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setB64Document(String document) {
		this.b64Document = document;
	}

	public String getB64Document() {
		return b64Document;
	}

	public void setDocumentType(String dtype) {
		this.documentType = dtype;
	}

	public String getDocumentType() {
		return documentType;
	}

	public String getSignerRID() {
		return signerRID;
	}

	public void setSignerRID(String signerRID) {
		this.signerRID = signerRID;
	}

	public String getCertType() {
		return certType;
	}

	public void setCertType(String certType) {
		this.certType = certType;
	}
}
