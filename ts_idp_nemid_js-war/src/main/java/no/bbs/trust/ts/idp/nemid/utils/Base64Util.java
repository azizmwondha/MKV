package no.bbs.trust.ts.idp.nemid.utils;

import java.io.UnsupportedEncodingException;
import org.bouncycastle.util.encoders.Base64;

public class Base64Util {

	public static byte[] encode(byte[] bytes) {
		return Base64.encode(bytes);
	}

	public static String encode(byte[] bytes, String encoding) throws UnsupportedEncodingException {
		return new String(Base64.encode(bytes), encoding);
	}

	public static byte[] decode(String encoded) {
		return Base64.decode(encoded);
	}

	public static String decode(String encoded, String encoding) throws UnsupportedEncodingException {
		return new String(Base64.decode(encoded), encoding);
	}

}
