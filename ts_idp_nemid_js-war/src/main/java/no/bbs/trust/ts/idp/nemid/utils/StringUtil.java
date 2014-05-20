package no.bbs.trust.ts.idp.nemid.utils;

import java.util.StringTokenizer;

public class StringUtil
{
	/**
	 * Removes CR/LF from the input String
	 *
	 * @param text String to manipulate
	 * @return String The resulting String or null
	 */
	public static String removeCRLF(String text)
	{
		if (null == text)
		{
			return null;
		}
		StringBuffer temp = new StringBuffer(text.length());

		for (int i = 0; i < text.length(); i++)
		{
			if ((text.charAt(i) != '\r') && (text.charAt(i) != '\n'))
			{
				temp.append(text.charAt(i));
			}
		}
		return temp.toString();
	}

	/**
	 * Strip all whitespace characters away from input String.
	 *
	 * @param i
	 * @return
	 */
	public static String stripWhitespaces(String i)
	{
		if (i != null)
		{
			StringTokenizer st = new StringTokenizer(i, "\r\n\t ", false);
			StringBuffer sb = new StringBuffer();

			while (st.hasMoreTokens())
			{
				sb.append(st.nextToken().trim());
			}

			return sb.toString();
		}

		return "";
	}
}
