package no.bbs.trust.ts.idp.nemid.utils;

public interface Checksum {

	boolean matches(Checksum checksum) throws Exception;

	void update(byte[] b, int offset, int count);

	byte[] digest();

}
