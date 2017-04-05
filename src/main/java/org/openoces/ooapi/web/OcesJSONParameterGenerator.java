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
package org.openoces.ooapi.web;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openoces.ooapi.utils.Base64Handler;
import org.openoces.serviceprovider.ServiceProviderException;


/**
* Generates valid JSON of the parameters for the JSClient.
*/
public class OcesJSONParameterGenerator {

	private static final String DIGEST = "PARAMS_DIGEST";
	private static final String SIGNATURE = "DIGEST_SIGNATURE";

	private final Signer signer;
	
	/**The collection of parameters. All of these will be included in the digest. Sorted non-case-sensitively.*/
    private final SortedMap<String, String> parameters = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    public OcesJSONParameterGenerator(Signer signer){
		this.signer = signer;
    }

    /**
     * Sets the parameter CLIENTFLOW before generating the JSON.
     * @return
     */
    public String getSignParametersJSON() {
		try {
			return getParametersAsJSON(true);
		} catch (ServiceProviderException spe) {
			throw new RuntimeException(spe);
		}
	}
    
    /**
     * Sets the parameter CLIENTFLOW before generating the JSON.
     * @return
     */
	public String getLogonParametersJSON() {
		try {
			return getParametersAsJSON(false);
		} catch (ServiceProviderException spe) {
			throw new RuntimeException(spe);
		}
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

    private String getParametersAsJSON(boolean sign) throws ServiceProviderException {
		addParameter("SP_CERT", signer.getCertificate());
		addClientflowParam(sign);
		
		byte[] normalizedParameters = getNormalizedParameters();
		byte[] parameterDigest = calculateDigest(normalizedParameters);
		byte[] parameterSignature = signer.calculateSignature(normalizedParameters);
		
		String digestString = Base64Handler.encode(parameterDigest);
		String signatureString = Base64Handler.encode(parameterSignature);
		
		JSONObject o = new JSONObject(); 
        try {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                o.put(entry.getKey(), entry.getValue());
            }
            o.put(DIGEST, digestString);
            o.put(SIGNATURE, signatureString);
        } catch (JSONException e) {
            throw new ServiceProviderException(e);
        }
		return o.toString();
	}

	private void addClientflowParam(boolean sign){
		addParameter("CLIENTFLOW", sign?"OCESSIGN2":"OCESLOGIN2");
	}

	/**
	 * Adds a parameter (key-value pair) to the internal collection. The parameter will be included when calculating the digest.
	 * @param key
	 * @param value
	 */
	public void addParameter(String key, String value){
		parameters.put(key, value);
	}

	protected byte[] calculateDigest(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA256").digest(data);
		} catch (Exception e) {
			throw new RuntimeException("Could not calculate digest", e);
		}
	}
	
	protected byte[] getNormalizedParameters() {
		StringBuffer sb = new StringBuffer();
        for (String key: parameters.keySet()){
            sb.append(key);
            sb.append(parameters.get(key));
        }
		try {
			return sb.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}	
}
