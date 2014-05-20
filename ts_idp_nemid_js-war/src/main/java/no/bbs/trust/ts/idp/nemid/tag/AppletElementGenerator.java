/*
 Copyright 2010 DanID

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

import no.bbs.trust.ts.idp.nemid.attachments.Attachments;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.utils.Base64;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.ts.idp.nemid.attachments.Attachment;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.MerchantPkiConfigDAO;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.MerchantPkiConfig;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 * Assists with generating the applet tag for the service provider demo applet.
 */
public class AppletElementGenerator
{	
	/**
	 * Denne parameter kan bruges på Windows-platformen til kun at vise bestemte 
	 * certifikater fra brugerenscertificatstore.
	 *
	 * Hvis man angiver: <param name="subjectdnfilter" value="UElEOg=="/> 
	 * Vil kun de POCES certifikater, som ligger i certificatstore blive
	 * vist.
	 *
	 * Værdien "UElEOg==" er base64-kodning af ”PID:”, og kun POCES-certifikater har "PID:" som delstreng i subjectdn. 
	 * Hvis man i stedet angiver "UklEOg==", Hvis man ønsker muligheden for både at logge ind med MOCES og POCES, 
	 * bør subjectdnfilter være tom, således at både certifikater med henholdsvis ”RID:” og ”PID:”i subjectdn 
	 * i certificatstore, vil blive vist i dropdown boksen i OpenSign.
	 */
	
	private final SortedMap<String, String> signedParameters = new TreeMap<String, String>(new Comparator<String>()
	{
		public int compare(String o1, String o2)
		{
			return o1.compareToIgnoreCase(o2);
		}
	});
	private final SortedMap<String, String> unsignedParameters = new TreeMap<String, String>();
	private final Signer signer;
	private String serverUrlPrefix;
	private String signChallenge;

	public AppletElementGenerator(Signer signer)
	{
		this.signer = signer;
	}

	public void setServerUrlPrefix(String serverUrlPrefix)
	{
		this.serverUrlPrefix = serverUrlPrefix;
	}

	public void setAttachments(Attachments attachments)
	{
		try
		{
			String encodedXML = no.bbs.trust.common.basics.utils.Base64.encode(attachments.toXML(), "UTF-8", false);
			addSignedParameter("attachments", encodedXML);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setPDF(Attachment pdf)
	{
		addSignedParameter("signtext.uri", pdf.getPath());
		addSignedParameter("signtext.hash.value", pdf.getB64HashValue());
//		addSignedParameter("signtext.uri.hash.algorithm", pdf.getB64HashAlgo());
		addSignedParameter("signtextformat", "pdf");
		addSignedParameter("signtext.chunk", "true");
	}

	public void setChallenge(String signChallenge)
	{
		addSignedParameter("signproperties", "challenge=" + signChallenge);
		this.signChallenge = signChallenge;
	}

	public void setLogLevel(String logLevel)
	{
		addSignedParameter("log_level", logLevel); // INFO/DEBUG/ERROR
	}

	public void setSignText(String signText, String format)
	{
		try
		{
			String encodedText = Base64.encode(signText, "UTF-8", false);
			addSignedParameter("signtext", encodedText);
			addSignedParameter("signtextformat", format);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String generateSignAppletElement(String sref, String subjectDnFilter, String verifyURL)
	{
		addSignedParameter("paramcert", signer.getCertificate());
		addSignedParameter("ZIP_FILE_ALIAS", "OpenSign2");
		addSignedParameter("ZIP_BASE_URL", getServerUrlPrefix());
		addSignedParameter("ServerUrlPrefix", getServerUrlPrefix());

		byte[] normalizedParameters = getNormalizedParameters();
		byte[] parameterDigest = calculateDigest(normalizedParameters);
		byte[] parameterSignature = signer.calculateSignature(normalizedParameters);
		
		Logger.getLogger(Constants.MAIN_LOGGER).trace("Normalized params: [" + new String (normalizedParameters) + "]");

		String digestString = new String(Base64.encode(parameterDigest, false));
		String signatureString = new String(Base64.encode(parameterSignature, false));

		addUnsignedParameter(ConfigKeys.CONFIG_NEMID_TAG_DIGEST, digestString);
		addUnsignedParameter(ConfigKeys.CONFIG_NEMID_TAG_SIGNATURE, signatureString);
		
		if (null != subjectDnFilter)
		{
			addUnsignedParameter(ConfigKeys.CONFIG_NEMID_TAG_SUBJECTDNFILTER, subjectDnFilter);
		}
		addUnsignedParameter("MAYSCRIPT", "true");

		// Create a unique path to the applet, to prevent caching in the applet loader
		String appletPath = getServerUrlPrefix() + "/bootapplet/" + System.currentTimeMillis();

		String tag = Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_TAG_SOURCE);

		try {
			int midval = Integer.parseInt((String)MDC.get("MID"));
			MerchantPkiConfigDAO conf = new MerchantPkiConfigDAO();
			MerchantPkiConfig height = conf.getByMerchantidPkiidKey(null, midval, 8, "height");
			MerchantPkiConfig width = conf.getByMerchantidPkiidKey(null, midval, 8, "width");
			if(height != null) {
				tag = StringUtils.replace(tag, "nemid.applet.height", height.getValue());
			}
			if(width != null) {
				tag = StringUtils.replace(tag, "nemid.applet.width", width.getValue());
			}
		} catch(Exception e) {
			Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER).info("Cannot parse merchant config for applet dimensions: " + e.getMessage());
		}

		tag = StringUtils.replace(tag, "nemid.clienttag.sref", sref);
		tag = StringUtils.replace(tag, "nemid.applet.width", Config.INSTANCE.getProperty("nemid.applet.width"));
		tag = StringUtils.replace(tag, "nemid.applet.height", Config.INSTANCE.getProperty("nemid.applet.height"));
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_APPLETPATH), appletPath);
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_VERIFYURL), verifyURL);
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_STATUSURL), verifyURL);
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_CHALLENGE), this.signChallenge);

		StringBuilder signedParams = new StringBuilder();

		for (Map.Entry<String, String> entry : signedParameters.entrySet())
		{
			signedParams.append(toAppParamsTag(entry));
		}
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_PARAMS_SIGNED), signedParams.toString());

		StringBuilder unsignedParams = new StringBuilder();
		
		for (Map.Entry<String, String> entry : unsignedParameters.entrySet())
		{
			unsignedParams.append(toAppParamsTag(entry));
		}
		tag = StringUtils.replace(tag, (ConfigKeys.CONFIG_NEMID_TAG_PARAMS_UNSIGNED), unsignedParams.toString());

		Logger.getLogger(Constants.MAIN_LOGGER).trace("NemID activation tag: " + tag);
		
		return tag;
	}

	private String getServerUrlPrefix()
	{
		return serverUrlPrefix;
	}

	protected String toAppParamsTag(Map.Entry<String, String> entry)
	{
		return "<param name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" />\n";
	}

	public void addSignedParameter(String key, String value)
	{
		signedParameters.put(key, value);
	}

	public void addUnsignedParameter(String key, String value)
	{
		unsignedParameters.put(key, value);
	}
	
	protected byte[] calculateDigest(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA256", "BC").digest(data);
		} catch (Exception e) {
			throw new RuntimeException("Could not calculate digest", e);
		}
	}	

	protected byte[] getNormalizedParameters()
	{
		StringBuilder sb = new StringBuilder();
		for (String key : signedParameters.keySet())
		{
			sb.append(key.toLowerCase() + signedParameters.get(key));
		}
		try
		{
			return sb.toString().getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}
}
