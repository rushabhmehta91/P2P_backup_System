import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class poc {
	static String username = "kansas";
	static String password = "kansas";

	public static SecretKey calculateUserKey(String user, String pass)
			throws NoSuchAlgorithmException {

		String keyPhrase = username + password;

		String keyHash = Utilities.sha1(keyPhrase);
		System.out.println(keyHash);
		byte[] keyHashBytes = keyHash.getBytes();

		byte key[] = Arrays.copyOf(keyHashBytes, 16);

		SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

		return secretKey;
	}

	public static void encrypt(String fname) throws Exception {
		// KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		// keyGen.init(256); // AES-256
		// SecretKey key = keyGen.generateKey();

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

				String path = f.getParent() + "//" + name + "."
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

	public static void join(String baseFilename, int numberParts,
			String newFileName) throws IOException {

		String path = "/home/mandeep/";
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(path + newFileName));

		for (int part = 0; part < numberParts; part++) {

			String partFile = String.format(path + baseFilename + "." + "%03d",
					part + 1);

			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(partFile));

			int b;
			while ((b = in.read()) != -1)
				out.write(b);

			in.close();
		}
		out.close();
	}

	public static void decrypt(String fname) throws Exception {

		SecretKey key = calculateUserKey(username, password);

		// creating file input stream to read from file
		try (FileInputStream fis = new FileInputStream(fname)) {
			// creating object input stream to read objects from file
			ObjectInputStream ois = new ObjectInputStream(fis);

			// key = (SecretKey) ois.readObject(); // reading key used for
			// encryption
			//key = calculateUserKey(username, password);
			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.DECRYPT_MODE, key);

			try (FileOutputStream fos = new FileOutputStream(
					"/home/mandeep/pap" + ".pdf")) {
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

	public static void main(String[] args) throws Exception {

		String path = "/home/mandeep/";
		String fileName = "me.pdf";

		//encrypt(path + "/" + fileName);
		decrypt(path + fileName);

	}

}