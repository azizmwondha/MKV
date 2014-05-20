package no.bbs.trust.ts.idp.nemid.utils;

public final class HexDump {
	static char[] hexstr = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	/**
	 * Convert byte value to hexadecimal string representation (for trace dumps)
	 * @return java.lang.String
	 * @param value byte
	 * @param digits int
	 */
	static String tohex(byte value, int digits) {
		return tohex((int) value, digits);
	}
	/**
	 * Convert byte value to hexadecimal string representation (for trace dumps)
	 * @return java.lang.String
	 * @param value int
	 * @param digits int
	 */
	public static String tohex(int value, int digits) {
		char[] ret = new char[digits];
		int n;
		byte a;

		for (n = 0; n < digits; n++) {
			a = (byte) (value & 0x0F);
			value >>= 4;

			ret[digits - n - 1] = hexstr[a];
		}

		return String.valueOf(ret);
	}
	
	public static String xdump(byte[] bytes) {
		return xdump(bytes, 0, bytes.length);
	}
	
	/**
	 * Dumps data block in hexadecimal representation (part of trace action)
	 * @param bytes byte[]
	 */
	public static String xdump(byte[] bytes, int srcPos, int length) {
		if (bytes==null)
			return "null";
		int len = length;
		if (srcPos+length>bytes.length)
			len = bytes.length;
		int ofs = srcPos, count = 0;
		int n;
		StringBuffer sb = new StringBuffer(80);
		StringBuffer outstr = new StringBuffer(5 * len);

		while (ofs < len) {
			count = (len - ofs) < 16 ? (len - ofs) : 16;

			sb.setLength(0);

			sb.append(tohex(ofs, 4));
			sb.append(": ");

			// First, the hex bytes
			for (n = 0; n < count; n++) {
				if (n == 8)
					sb.append("- ");
				sb.append(tohex((int) bytes[ofs + n], 2));
				sb.append(' ');
			}

			// Then fill up with spaces
			for (n = count; n < 16; n++)
				sb.append("   ");

			if (count < 9) // Add the '- ' if we need it.
				sb.append("  ");

			// Seperate hex bytes from ascii chars
			sb.append(" ");

			// And last, the ascii characters
			for (n = 0; n < count; n++) {
				char b = (char) bytes[ofs + n];

				if (b >= (char) 32 && (char) b <= 127)
					sb.append(b);
				else
					sb.append('.');
			}

			sb.append("\r\n");
			outstr.append(sb.toString());
			//		log(sb.toString());
			ofs += count;
		}

		return outstr.toString();
	}
	
	public static String hex(byte[] bytes) {
		if (bytes==null)
			return "null";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i++)
			sb.append(tohex(bytes[i], 2));
		return "0x" + sb.toString().toLowerCase();
	}
}
