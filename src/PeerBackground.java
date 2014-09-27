import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class PeerBackground implements Runnable {

	private static ServerSocket listeningSocket = null;

	PeerBackground(int portForCAN) throws IOException {
		
		listeningSocket = new ServerSocket(portForCAN);
		
	}

	public void run() {
		while (true) {
			try {
				Socket client = listeningSocket.accept();

				// Start listening for CAN messages.
				new Thread(new PeerWorker(client, Peer.portforCAN)).start();

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			continue;
		}
	}

	public static void stopListening() {
		try {
			System.out.println("Stopping monitoring in CAN.");
			listeningSocket.close();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}
