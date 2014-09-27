
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Utilities {

	public static SecretKey calculateUserKey(String user, String pass)
			throws NoSuchAlgorithmException {

		String keyPhrase = user + pass;

		String keyHash = Utilities.sha1(keyPhrase);

		byte[] keyHashBytes = keyHash.getBytes();

		byte key[] = Arrays.copyOf(keyHashBytes, 16);

		SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

		return secretKey;
	}

	public static int getAvailablePort() throws IOException {
		int port = 0;
		Random r = new Random();
		do {
			port = r.nextInt(20000) + 10000;
		} while (!isPortAvailable(port));

		return port;
	}

	private static boolean isPortAvailable(final int port) throws IOException {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			return true;
		} catch (final IOException e) {
		} finally {
			if (ss != null) {
				ss.close();
			}
		}

		return false;
	}

	/*
	 * Get SHA-1 hash of file name
	 */
	public static String sha1(String input) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16)
					.substring(1));
		}

		return sb.toString();
	}

	public static void encrypt(String fname, String username, String password)
			throws Exception {

//		System.out.println("Encrypting " + fname + " with " + username + " "
//				+ "****");

		SecretKey key = calculateUserKey(username, password);

		Cipher aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.ENCRYPT_MODE, key);

		// creating file output stream to write to file
		try (FileOutputStream fos = new FileOutputStream(fname + ".enc")) {
			// creating output stream to write to file
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			// oos.writeObject(key);

			// creating file input stream to read contents
			try (FileInputStream fis = new FileInputStream(fname)) {

				// creating cipher output stream to write encrypted contents
				try (CipherOutputStream cos = new CipherOutputStream(fos,
						aesCipher)) {
					int read;
					byte buf[] = new byte[4096];
					while ((read = fis.read(buf)) != -1)
						// Writing to file as cipher
						cos.write(buf, 0, read);
				}
			}
		}
	}

	public static void splitFile(String fileName, int parts) throws IOException {
		try {
//			System.out.println("Splitting file :" + fileName);
			File f = new File(fileName);
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(f));
			FileOutputStream out;
			String name = f.getName();
			int partCounter = 1;
			int sizeOfFiles = (int) (f.length() / parts) + 1;
			byte[] buffer = new byte[sizeOfFiles];
			int tmp = 0;
			while ((tmp = bis.read(buffer)) > 0) {

				String path = f.getParent() + "/" + name + "."
						+ String.format("%03d", partCounter++);
				File newFile = new File(path);
				newFile.createNewFile();
				out = new FileOutputStream(newFile);
				out.write(buffer, 0, tmp);
				out.close();
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	public static void join(String path, String baseFilename, int numberParts,
			String newFileName) throws IOException {

		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(path + newFileName));

		for (int part = 0; part < numberParts; part++) {

			String partFile = String.format(path +"/" + baseFilename + "."
					+ "%03d", part + 1);

			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(partFile));

			int b;
			while ((b = in.read()) != -1)
				out.write(b);

			in.close();
		}
		out.close();
	}

	public static void decrypt(String fname, String username, String password,
			String finalName) throws Exception {

		String path = Peer.getDir()  + Peer.hostName + "/ret/";

		SecretKey key = calculateUserKey(username, password);
//		System.out.println("Attempting to decrypt with " + username + " and "
//				+ password);
//System.out.println(fname);
		// creating file input stream to read from file
		try (FileInputStream fis = new FileInputStream(fname)) {
			ObjectInputStream ois = new ObjectInputStream(fis);

			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.DECRYPT_MODE, key);

			try (FileOutputStream fos = new FileOutputStream(path + finalName)) {
				// creating cipher input stream to read encrypted contents
				try (CipherInputStream cis = new CipherInputStream(fis,
						aesCipher)) {
					int read;
					byte buf[] = new byte[4096];
					while ((read = cis.read(buf)) != -1)
						// Writing to file in Decrypt mode
						fos.write(buf, 0, read);
				}
			}
		}

	}
}