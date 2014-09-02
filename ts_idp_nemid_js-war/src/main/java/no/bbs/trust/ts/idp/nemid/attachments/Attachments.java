package no.bbs.trust.ts.idp.nemid.attachments;

import java.util.ArrayList;

public class Attachments {

	private static String XML_PROCESSING_INSTRUCTION = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
	private static String XML_ELEM_ATTACHMENTS = "attachments";
	private ArrayList<Attachment> attachments = null;

	public Attachments() {
		attachments = new ArrayList<Attachment>();
	}

	public String toXML() {
		StringBuffer b = new StringBuffer(100);
		b.append(XML_PROCESSING_INSTRUCTION);
		b.append(startTag(XML_ELEM_ATTACHMENTS));
		for (Attachment attachment : attachments) {
			b.append(attachment.toXML());
		}
		b.append(endTag(XML_ELEM_ATTACHMENTS));
		return b.toString();
	}

	public void addAttachment(Attachment att) {
		if (null == attachments) {
			attachments = new ArrayList<Attachment>();
		}
		attachments.add(att);
	}

	private String startTag(String xmlElem) {
		return "<" + xmlElem + ">";
	}

	private String endTag(String xmlElem) {
		return "</" + xmlElem + ">";
	}

}
