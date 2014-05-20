/*
    Copyright 2010 Nets DanID

    This file is part of OpenOcesAPI.

    OpenOcesAPI is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    OpenOcesAPI is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with OpenOcesAPI; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    Note to developers:
    If you add code to this file, please take a minute to add an additional
    @author statement below.
*/
package no.bbs.trust.ts.idp.nemid.tag;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;

import org.openoces.ooapi.utils.Base64Handler;
import org.openoces.ooapi.web.JSONException;
import org.openoces.ooapi.web.JSONObject;

import eu.nets.no.vas.esign.sdosigner.types.KeyCredentials;

/**
* Generates valid JSON of the parameters for the JSClient.
*/
public class OcesJSONParameterGenerator {

	private static final String DIGEST = "PARAMS_DIGEST";
	private static final String SIGNATURE = "DIGEST_SIGNATURE";

	private final Signer signer;

	/**
	 * The collection of parameters. All of these will be included in the digest. Sorted non-case-sensitively.
	 */
	private final SortedMap<String, String> parameters = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

	public OcesJSONParameterGenerator(Signer signer) {
		this.signer = signer;
	}

	/**
	 * Sets the parameter CLIENTFLOW before generating the JSON.
	 * @return the signing parameters in JSON format
	 */
	public String getSignParametersJSON() {
		return getParametersAsJSON(true);
	}

	/**
	 * Sets the parameter CLIENTFLOW before generating the JSON.
	 * @return the logon parameters in JSON format
	 */
	public String getLogonParametersJSON() {
		return getParametersAsJSON(false);
	}

	private String getParametersAsJSON(boolean sign) {
		addClientflowParam(sign);
		return getParametersAsJSON();
	}

	public String getParametersAsJSON() {
		addParameter("SP_CERT", signer.getCertificate());

		byte[] normalizedParameters = getNormalizedParameters();
		byte[] parameterDigest = calculateDigest(normalizedParameters);
		byte[] parameterSignature = signer.calculateSignature(normalizedParameters);

		JSONObject o = new JSONObject();
		try {
			for (Map.Entry<String, String> entry : parameters.entrySet()) {
				o.put(entry.getKey(), entry.getValue());
			}

			o.put(DIGEST, Base64Handler.encode(parameterDigest));
			o.put(SIGNATURE, Base64Handler.encode(parameterSignature));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return o.toString();
	}

	/**
	 * Utility method that Base64-encodes the received sign text and inserts the SIGNTEXT and SIGNTEXT_FORMAT parameters in the internal parameter collection.
	 * @param signText
	 * @param format
	 */
	public void setSignText(String signText, String format) {
		addParameter("SIGNTEXT", Base64Handler.encode(signText));
		addParameter("SIGNTEXT_FORMAT", format);
	}

	private void addClientflowParam(boolean sign) {
		addParameter("CLIENTFLOW", sign ? "OCESSIGN2" : "OCESLOGIN2");
	}

	/**
	 * Adds a parameter (key-value pair) to the internal collection. The parameter will be included when calculating the digest.
	 * @param key the key of the parameter to add
	 * @param value the value of the parameter to add
	 */
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	protected static byte[] calculateDigest(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA256").digest(data);
		} catch (Exception e) {
			throw new RuntimeException("Could not calculate digest", e);
		}
	}

	protected byte[] getNormalizedParameters() {
		StringBuffer sb = new StringBuffer();
		for (String key : parameters.keySet()) {
			sb.append(key);
			sb.append(parameters.get(key));
		}
		try {
			return sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String generateClientTag(String mid, String clientMode, String language, String sref) throws StatusCodeException {
		return generateParametersTag(mid, clientMode, language) + System.getProperty("line.separator") + generateIframeTag()
				+ System.getProperty("line.separator") + generateScriptTag() + System.getProperty("line.separator") + generatePostBackFormTag(sref);
	}

	private static String generateParametersTag(String mid, String clientMode, String language) throws StatusCodeException {
		KeyCredentials credentials = null;
		try {
			credentials = DAOUtil.getMerchantCredentials(mid);
		} catch (SQLException ex) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to obtain key credentials for Merchant [" + mid + "] "
					+ ex.getMessage());
		}

		Signer signer = new Signer(credentials.getKeystorepath(), credentials.getKeystorepass(), credentials.getKeyalias(), credentials.getKeyaliaspass());
		OcesJSONParameterGenerator generator = new OcesJSONParameterGenerator(signer);

		// Set parameter values
		generator.addParameter("CLIENTFLOW", Config.INSTANCE.getProperty("nemid.client.clientflow.signing"));
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
		generator.addParameter("TIMESTAMP", String.valueOf(dateFormat.format(new Date())));
		generator.addParameter("CLIENTMODE", clientMode);
		generator.addParameter("LANGUAGE", language);
		generator.addParameter("SIGNTEXT_FORMAT", "text");
		generator.addParameter("SIGNTEXT", Base64Handler.encode("Text to sign"));

		return String.format(Config.INSTANCE.getProperty("nemid.clienttag.parameters"), generator.getParametersAsJSON());
	}

	private static String generateIframeTag() {
		String iframeTag = Config.INSTANCE.getProperty("nemid.clienttag.iframe");
		iframeTag = String.format(iframeTag, Config.INSTANCE.getProperty("nemid.client.width"), Config.INSTANCE.getProperty("nemid.client.height"),
				Config.INSTANCE.getProperty("nemid.client.launcher") + System.currentTimeMillis());
		return iframeTag;
	}

	private static String generateScriptTag() {
		String scriptTag = Config.INSTANCE.getProperty("nemid.clienttag.script");
		scriptTag = String.format(scriptTag, Config.INSTANCE.getProperty("nemid.client.origin"), Config.INSTANCE.getProperty("nemid.client.url"));
		return scriptTag;
	}

	private static String generatePostBackFormTag(String sref) {
		String postBackFormTag = Config.INSTANCE.getProperty("nemid.clienttag.postbackform");
		postBackFormTag = String.format(postBackFormTag, Config.INSTANCE.getProperty("nemid.url.receipt"), sref);
		return postBackFormTag;
	}

}
