package edu.pku.sxt.pkuc.client.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 Checksum Utility
 * @author songxintong
 *
 */
public class MD5 {
	
	public static String md5(byte[] data) throws MD5Exception {
		// calculate MD5 checksum 
		byte[] md5;
		try {
			md5 = MessageDigest.getInstance("MD5").digest(data);
		} catch (NoSuchAlgorithmException e) {		
			throw new MD5Exception("MD5 Algorithm is not supported.");
		} 
		// convert MD5 checksum to String
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < md5.length; i++){
			if (Integer.toHexString(0xFF & md5[i]).length() == 1)
				str.append("0").append(Integer.toHexString(0xFF & md5[i]));
			else
				str.append(Integer.toHexString(0xFF & md5[i]));
		}
		return str.toString();
	}
	
	public static String md5(File file) throws MD5Exception {
		// read file into byte[]
		FileInputStream fis;
		ByteArrayOutputStream baos;
		try {
			fis = new FileInputStream(file);
			baos = new ByteArrayOutputStream(1024);
			byte[] temp = new byte[1024];
			int size = 0;
			while ((size = fis.read(temp)) != -1) {
				baos.write(temp, 0, size);
			}
			fis.close();
		} catch (IOException e) {
			throw new MD5Exception("Failed to read file: "+file.getName());
		}
		// calculate MD5 checksum and return
		return md5(baos.toByteArray());
	}	
}