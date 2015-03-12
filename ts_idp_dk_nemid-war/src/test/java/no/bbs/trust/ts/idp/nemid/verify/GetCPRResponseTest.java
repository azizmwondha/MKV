package no.bbs.trust.ts.idp.nemid.verify;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import no.bbs.trust.ts.rid.client.soap.GetCPRResponse;

public class GetCPRResponseTest extends TestCase {

	/**
	 * @param args
	 */
	public void testConstructor() {
//		System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
//		System.setProperty("jaxp.debug", "1");
//		System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<soapenv:Body><ns1:getCPRResponse soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
				"xmlns:ns1=\"http://localhost/\"><result xsi:type=\"xsd:string\" xsi:nil=\"true\"/></ns1:getCPRResponse>" +
				"</soapenv:Body></soapenv:Envelope>";
		try {
			GetCPRResponse resp = new GetCPRResponse(response.getBytes("UTF-8"));
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
