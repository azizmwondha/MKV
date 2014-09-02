package no.bbs.trust.ts.idp.nemid.attachments;

public class Attachment {

	private static String XML_ELEM_ATTACHMENT = "attachment";
	private static String XML_ELEM_TITLE = "title";
	private static String XML_ELEM_PATH = "path";
	private static String XML_ELEM_MIMETYPE = "mimeType";
	private static String XML_ELEM_SIZE = "size";
	private static String XML_ELEM_HASHVALUE = "hashValue";
	private static String XML_ELEM_HASHALGO = "hashAlgo";
	private static String MIMETYPE_PDF = "text/pdf";

	private String title = null;
	private String path = null;
	private String mimeType = null;
	private int size = -1;
	private String b64HashAlgo = null;
	private String b64HashValue = null;

	public String toXML() {
		StringBuffer b = new StringBuffer(100);
		b.append(startTag(XML_ELEM_ATTACHMENT));
		if (null != getTitle()) {
			b.append(startTag(XML_ELEM_TITLE));
			b.append(getTitle());
			b.append(endTag(XML_ELEM_TITLE));
		}
		if (null != getPath()) {
			b.append(startTag(XML_ELEM_PATH));
			b.append(getPath());
			b.append(endTag(XML_ELEM_PATH));
		}
		if (null != getMimeType()) {
			b.append(startTag(XML_ELEM_MIMETYPE));
			b.append(getMimeType());
			b.append(endTag(XML_ELEM_MIMETYPE));
		}
		if (-1 != getSize()) {
			b.append(startTag(XML_ELEM_SIZE));
			b.append(getSize());
			b.append(endTag(XML_ELEM_SIZE));
		}
		if (null != getB64HashValue()) {
			b.append(startTag(XML_ELEM_HASHVALUE));
			b.append(getB64HashValue());
			b.append(endTag(XML_ELEM_HASHVALUE));
		}
		if (null != getB64HashAlgo()) {
			b.append(startTag(XML_ELEM_HASHALGO));
			b.append(getB64HashAlgo());
			b.append(endTag(XML_ELEM_HASHALGO));
		}

		b.append(endTag(XML_ELEM_ATTACHMENT));
		return b.toString();
	}


	public String getTitle() {
		return title;
	}

	public void setTitle(String t) {
		title = t;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String t) {
		path = t;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String t) {
		mimeType = t;
	}

	public String getB64HashValue() {
		return b64HashValue;
	}

	public void setB64HashValue(String t) {
		b64HashValue = t;
	}

	public String getB64HashAlgo() {
		return b64HashAlgo;
	}

	public void setB64HashAlgo(String t) {
		b64HashAlgo = t;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int t) {
		size = t;
	}


	private String startTag(String xmlElem) {
		return "<" + xmlElem + ">";
	}

	private String endTag(String xmlElem) {
		return "</" + xmlElem + ">";
	}


}
