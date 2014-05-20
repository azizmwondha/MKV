package no.bbs.trust.ts.idp.nemid.tag;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ChallengeGenerator
{
	public static String generateChallenge()
	{
		String challenge;
		try
		{
			challenge = "" + SecureRandom.getInstance("SHA1PRNG").nextLong();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
		return challenge;
	}
}
