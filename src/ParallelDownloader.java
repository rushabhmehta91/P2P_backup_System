import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ParallelDownloader implements Runnable {

	String fileName, location, cleanName;

	public ParallelDownloader(String fileName, String location, String cleanName) {
		this.fileName = fileName;
		this.location = location;
		this.cleanName = cleanName;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		int port = 0;

		System.out.println("Parallel download for :" + fileName + " from "
				+ location);
		try {
			// Open fresh port to receive file.
			port = Utilities.getAvailablePort();

			// Send pull request
			String host[] = location.split(":");

			CANMessage downloadRequest = new CANMessage("sendfile", fileName,
					port);
			Socket socket = new Socket(host[0], Integer.parseInt(host[1]));
			ObjectOutputStream o = new ObjectOutputStream(
					socket.getOutputStream());
			o.writeObject(downloadRequest);

			ServerSocket serv = new ServerSocket(port);
			Socket receiver = serv.accept();

			// Download file and write to ret folder.
			File downloadedFile = new File(Peer.getDir() + "/" + Peer.hostName
					+ "/ret/" + cleanName);

			try {

				FileOutputStream fos = new FileOutputStream(downloadedFile,
						false);

				byte[] buffer = new byte[1024];
				int count;

				InputStream in = receiver.getInputStream();

//				System.out.println(in.available());

				while ((count = in.read(buffer)) != -1) {
					fos.write(buffer, 0, count);
				}
				fos.close();
				socket.close();
			} catch (Exception exp) {
				exp.printStackTrace();
			}

		} catch (NumberFormatException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			Thread.sleep(2000, 0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
