import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

class WorkerThread implements Runnable {

	private Socket client = null;
	InputStreamReader ipReader = null;
	BufferedReader input = null;
	PrintStream output = null;
	String entryhost;
	public static HashMap<String, String> users = new HashMap<String, String>();;

	public WorkerThread(Socket client, String entryPoint) {
		this.client = client;
		this.entryhost = entryPoint;
		//users = new HashMap<String, String>();
	}

	/**
	 * Get Input Output Streams from client socket
	 * 
	 * @throws java.io.IOException
	 */
	void openIOStreams() throws IOException {

		ipReader = new InputStreamReader(client.getInputStream());

		input = new BufferedReader(ipReader);

		output = new PrintStream(client.getOutputStream(), true);

	}

	/**
	 * Closes input and output streams.
	 * 
	 * @throws java.io.IOException
	 */
	void closeIOStreams() throws IOException {
		input.close();
		output.close();
	}

	public void run() {
		try {
			
			openIOStreams();
			System.out.println("before request");
			String request = input.readLine();
			System.out.println("after request  "+ request);
			if (request.equals("join")) {
				int port = Integer.parseInt(input.readLine());

				if (entryhost == null) {

					String[] host = client.getRemoteSocketAddress().toString()
							.split(":");
					String ip = host[0].substring(1);
					BootStrapServer.entryPoint = ip + ":"
							+ String.valueOf(port);
					output.println("owner");
				} else {
					output.println(entryhost);
					System.out.println("Sent out " + entryhost + " for entry");
				}
			}

			else {
				String host[] = request.split(":");
				String newEntry = host[0] + ":" + host[1];
				String c = client.getRemoteSocketAddress().toString()
						.split(":")[0];

				if (entryhost.split(":")[0].equalsIgnoreCase(c.substring(1))) {
					System.out.println("New entry is " + newEntry);
					BootStrapServer.setEntry(newEntry);
				}
			}

			closeIOStreams();

		} catch (IOException e) {
//			e.printStackTrace();
		}
	}

}

public class BootStrapServer implements Runnable {

	private int port = 8085;
	private ServerSocket serverSocket = null;
	private Thread currentThread = null;

	public static volatile String entryPoint = null;

	public BootStrapServer(int port) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(this.port);
	}

	public static void setEntry(String entry) {
		entryPoint = entry;
	}

	@Override
	public void run() {
		while (true) {

			Socket client = null;
			try {

				// Accept peer connection and assign to socket
				client = serverSocket.accept();

				InputStreamReader ipReader = new InputStreamReader(
						client.getInputStream());
				BufferedReader input = new BufferedReader(ipReader);

				PrintStream output = new PrintStream(client.getOutputStream());
				String command = input.readLine();
				System.out.println("Command is :" + command);
				if (command.equalsIgnoreCase("authenticate")) {
					while (!checkUser(input, output));
				}

				System.out.println("Entry point iiiisss: " + entryPoint);

				// Make new Worker for current client and start a thread for it.
				new Thread(new WorkerThread(client, entryPoint)).start();

			} catch (IOException e) {
				System.out.println("Error accepting connection.");
			}
		}
	}

	private boolean checkUser(BufferedReader input, PrintStream output)
			throws IOException {
		String command = input.readLine();

		String username = input.readLine();
		String password = input.readLine();
		if (command.equalsIgnoreCase("authenticate")) {

			if (WorkerThread.users.containsKey(username)) {
				if (WorkerThread.users.get(username).equalsIgnoreCase(password)) {
					output.println("loginsucc");
					return true;
				} else {
					output.println("wrongpass");
					return false;
				}
			} else {
				output.println("wronguser");
				return false;
			}
		} else {
			WorkerThread.users.put(username, password);
			output.println("signupsucc");
			return true;
		}

	}

	public static void main(String[] args) throws Exception {

		// Get port from command line.
		int port = Integer.parseInt(args[0]);
		//int port = 3252;
		// int port = 9998;
		// Initialize server to listen to port.
		BootStrapServer server = new BootStrapServer(port);
		server.run();

	}

}
