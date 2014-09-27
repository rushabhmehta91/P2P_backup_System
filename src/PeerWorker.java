import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;

class PeerWorker implements Runnable {

	Socket client = null;

	public PeerWorker(Socket temp, int port) throws UnknownHostException,
			IOException {
		client = temp;
	}

	public PeerWorker() {

	}

	// Generic metod to send object to address: both in arguments.
	public void sendMessageTo(String address, CANMessage message)
			throws NumberFormatException, UnknownHostException, IOException {
		String host[] = address.split(":");

		Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

		ObjectOutputStream out = new ObjectOutputStream(
				socket.getOutputStream());

		out.writeObject(message);
		out.close();

		socket.close();
	}

	public void greedyRoute(CANMessage message) {

		try {
			// Get neighbor nearest to destination
			ArrayList<ArrayList<Double>> distanceMatrix = getDistanceMatrix(message.destination);

			String nextHop = getNextHop(distanceMatrix);

			// Add self to path if search request.
			if (message.requestType.equalsIgnoreCase("search")
					|| message.requestType.equalsIgnoreCase("backuptarget"))
				message.path.add(Peer.CANid + " : " + Peer.hostName);
			sendMessageTo(nextHop, message);

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private String getNextHop(ArrayList<ArrayList<Double>> distanceMatrix) {
		int x = 0, y = 0;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < distanceMatrix.size(); i++) {

			// Calculating minimum distance to destination among neighbors
			ArrayList<Double> direction = distanceMatrix.get(i);
			for (int j = 0; j < direction.size(); j++) {
				if (direction.get(j) < min) {
					min = direction.get(j);
					x = i;
					y = j;
				}
			}
		}

		String nextHop = Peer.neighbours.get(x).get(y);
		return nextHop;
	}

	private ArrayList<ArrayList<Double>> getDistanceMatrix(Point destination)
			throws UnknownHostException, IOException {

		CANMessage getDistance = new CANMessage("distance", Peer.CANid,
				destination);

		ObjectOutputStream out;
		BufferedReader in;
		InputStreamReader ir;
		ArrayList<ArrayList<Double>> distanceMatrix = new ArrayList<ArrayList<Double>>();

		// Get distance of neighbors to destination.
		for (int i = 0; i < Peer.neighbours.size(); i++) {

			distanceMatrix.add(new ArrayList<Double>());
			ArrayList<String> direction = Peer.neighbours.get(i);
			if (direction.isEmpty())
				continue;

			for (int j = 0; j < direction.size(); j++) {
				String neighbor = direction.get(j);
				String host[] = neighbor.split(":");
				Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

				try {
					out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(getDistance);
					ir = new InputStreamReader(socket.getInputStream());
					in = new BufferedReader(ir);
					double reply = Double.parseDouble(in.readLine());
					out.close();
					in.close();

					// Waiting for reply from neighbor.
					distanceMatrix.get(i).add(reply);
					socket.close();
				} catch (Exception exp) {
					System.out.println(exp.getMessage());
				}
			}

		}
		return distanceMatrix;
	}

	double getDistanceToPoint(Point destination) {
		try {
			double distance = Peer.peerZone.getDistanceToPoint(destination);
			return distance;
		} catch (Exception exp) {
			return Double.MAX_VALUE;
		}
	}

	private CANMessage handOverTempZone(String node) {

		System.out.println("Attempting to transfer temp zone.");
		// Hand over entire temp zone.
		Zone newZone = Peer.tempZone;
		Peer.tempZone = null;
		// Hand over files in temp zone.
		HashMap<String, String> files = new HashMap<String, String>();

		Iterator<Map.Entry<String, String>> iter = Peer.files.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			Point hash = Peer.getHashFileName(entry.getKey());
			if (Peer.tempZone.isPointInZone(hash)) {
				files.put(entry.getKey(), entry.getValue());
				iter.remove();
			}
		}

		ArrayList<ArrayList<String>> neighboursToSend = new ArrayList<ArrayList<String>>();
		getCopyOfNeighbors(neighboursToSend);

		CANMessage welcome = new CANMessage("welcome", newZone,
				neighboursToSend, files);

		return welcome;

	}

	void InitiateNodeJoin(String node) {

		CANMessage welcome = null;
		boolean flag = false, trigger = false;
		ObjectOutputStream out;
		try {

			// Create new zone, neighbors and file list.
			Zone newZone = null;
			ArrayList<ArrayList<String>> neighboursToSend = new ArrayList<ArrayList<String>>();
			HashMap<String, String> files = new HashMap<String, String>();
			int count = 0;

			getCopyOfNeighbors(neighboursToSend);

			// Check if peer has temp zone it wants to hand over.
			if (Peer.tempZone != null) {
				welcome = handOverTempZone(node);
				flag = true;
				trigger = true;
			}

			else if (Peer.peerZone.isZoneSquare()
					|| Peer.peerZone.isBroaderThanTaller()) {
				newZone = Peer.peerZone.splitVertically();

				files = new HashMap<String, String>();

				Iterator<Map.Entry<String, String>> iter = Peer.files
						.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, String> entry = iter.next();
					Point hash = Peer.getHashFileName(entry.getKey());
					if (newZone.isPointInZone(hash)) {
						files.put(entry.getKey(), entry.getValue());
						iter.remove();
					}
				}

				// Clear all left neighbors.
				neighboursToSend.get(0).clear();
				// // Add self
				neighboursToSend.get(0).add(Peer.CANid + ":" + Peer.hostName);

				CANMessage message = new CANMessage("delete", Peer.CANid + ":"
						+ Peer.hostName, 0, Peer.peerZone);

				// Update this nodes right neighbors to remove it.
				for (String neighbor : Peer.neighbours.get(2)) {
					sendMessageTo(neighbor, message);
				}

				// Add new node to list of my neighbors on the right.
				Peer.neighbours.get(2).clear();
				Peer.neighbours.get(2).add(node);

			} else {

				newZone = Peer.peerZone.splitHorizontally();

				files = new HashMap<String, String>();

				Iterator<Map.Entry<String, String>> iter = Peer.files
						.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, String> entry = iter.next();
					Point hash = Peer.getHashFileName(entry.getKey());
					if (newZone.isPointInZone(hash)) {
						files.put(entry.getKey(), entry.getValue());
						iter.remove();
					}
				}

				// Clear all bottom neighbors.
				neighboursToSend.get(3).clear();
				// Add self as bottom neighbor
				neighboursToSend.get(3).add(Peer.CANid + ":" + Peer.hostName);
				CANMessage message = new CANMessage("delete", Peer.CANid + ":"
						+ Peer.hostName, 3, Peer.peerZone);
				// Update this nodes top neighbors to remove it.
				for (String neighbor : Peer.neighbours.get(1)) {
					sendMessageTo(neighbor, message);
				}

				// Add new node to list of my neighbors on top
				Peer.neighbours.get(1).clear();
				Peer.neighbours.get(1).add(node);

			}
			if (!flag)
				welcome = new CANMessage("welcome", newZone, neighboursToSend,
						files);

			// Send new zone, files and neighbors to new peer.
			sendMessageTo(node, welcome);

			if (trigger) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}
				Peer.informNeighborsOfExistenceTemp();
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	private void getCopyOfNeighbors(
			ArrayList<ArrayList<String>> neighboursToSend) {
		for (int i = 0; i < Peer.neighbours.size(); i++) {

			neighboursToSend.add(new ArrayList<String>());

			ArrayList<String> direction = Peer.neighbours.get(i);
			if (direction.isEmpty())
				continue;

			for (int j = 0; j < direction.size(); j++) {
				neighboursToSend.get(i).add(direction.get(j));
			}
		}
	}

	void serviceMessage(CANMessage message) throws IOException,
			ClassNotFoundException, NumberFormatException, InterruptedException {

		// Peer wants to join
		if (message.requestType.equals("join")) {

			InitiateNodeJoin(message.source);

			try {
				Thread.sleep(1000, 0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
			}

			Peer.informNeighborsOfExistence();
		}

		// Temporary merge for irregular join.
		if (message.requestType.equals("tempmerge")) {
			performTempMerge(message);
		}

		if (message.requestType.equals("area")) {
			// Get area.
			double area = Peer.peerZone.Area();

			PrintStream out = new PrintStream(client.getOutputStream());
			out.println(area);
		}

		else if (message.requestType.equals("found")
				|| message.requestType.equals("notfound")) {
			processSearchResult(message);
		} else if (message.requestType.equals("foundtarget")) {
			uploadFile(message);
		}

		// Calculating distance for greedy routing.
		else if (message.requestType.equals("distance")) {
			double distance = getDistanceToPoint(message.destination);

			PrintStream out = new PrintStream(client.getOutputStream());
			out.println(distance);

		}

		else if (message.requestType.equalsIgnoreCase("merge")) {
			performMerge(message);
		} else if (message.requestType.equalsIgnoreCase("check")) {
			checkForFile(message);
		}

		// Updating neighbors
		else if (message.requestType.equals("update")) {
			updateNeighbors(message);
		}
		// Deleting neighbors
		else if (message.requestType.equals("delete")) {
			Peer.neighbours.get(message.side).remove(message.source);
		}

		// In CAN, update neighbors and stuff.
		else if (message.requestType.equals("welcome")) {
			getJoinDetails(message);
			Peer.viewDetails();
		} else if (message.requestType.equals("sendfile")) {
			sendFile(message);
		}
		// Insert file at this location
		else if (message.requestType.equals("insert")) {
			insertFile(message);
		}

		// Insertion confirmation.
		else if (message.requestType.equals("inserted")) {
			System.out.println(message.message + " " + message.messageFrom);
		}

		else if (message.requestType.equals("search")) {
			checkForFile(message);
		} else if (message.requestType.equals("border")) {
			getBorderAndAreaStats(message);

		} else if (message.requestType.equalsIgnoreCase("backuptarget")) {

			ArrayList<String> path = new ArrayList<String>(message.path);
			path.add(Peer.CANid + " : " + Peer.hostName);
			CANMessage searchStatus = new CANMessage(path, "foundtarget",
					message.file, Peer.hostName + " " + Peer.CANid);

			sendMessageTo(message.source, searchStatus);

		}

		else if (message.requestType.equalsIgnoreCase("backup")) {

			File newFile = new File(Peer.getDir() + "/" + Peer.hostName + "/"
					+ message.message);

			try {
//				// File already exists here. Deleting file to maintain consistency.
//				if (newFile.exists()) {
//					System.out
//							.println("This file already exists. Deleting file to maintain fragment consistency");
//					newFile.delete();
//				}

				FileOutputStream fos = new FileOutputStream(newFile, false);

				byte[] buffer = new byte[1024];
				int count;

				InputStream in = client.getInputStream();

				// System.out.println(in.available());

				while ((count = in.read(buffer)) != -1) {
					fos.write(buffer, 0, count);
				}
				if (!newFile.getName().contains("_list")) 
					System.out.println(newFile.getName() + " received.");
				fos.close();
				client.close();
			} catch (Exception exp) {
				exp.printStackTrace();
			}

		}
	}

	private void sendFile(CANMessage message) throws NumberFormatException,
			UnknownHostException, IOException {

		String dirString = Peer.getDir() + "/" + Peer.hostName;

		String file = dirString + "/" + message.file;

		// System.out.println(client.getRemoteSocketAddress() + " wants " +
		// file);

		String requester = client.getRemoteSocketAddress().toString();
		requester = requester.substring(1);

		String host[] = requester.split(":");

		// System.out.println("Making connection to " + host[0] + " at " +
		// message.sendToPort);
		Socket s = new Socket(host[0], message.sendToPort);
		byte[] buffer = new byte[1024];
		int count;
		OutputStream out = s.getOutputStream();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				file));
		// System.out.println("sending " + file);
		while ((count = in.read(buffer)) != -1) {
			out.write(buffer, 0, count);
			out.flush();
		}
		// System.out.println("File sent");
		// file.delete();
		s.close();

	}

	private void updateNeighbors(CANMessage message) throws IOException {
		String reply = "no";

		if (Peer.isInCAN) {
			if (Peer.peerZone.isOverlap(message.zone)) {
				reply = "yes";
				if (!Peer.neighbours.get(message.side).contains(message.source)) {
					Peer.neighbours.get(message.side).add(message.source);
				}

			} else {
				Peer.neighbours.get(message.side).remove(message.source);
			}

			String source = message.source;
			String host[] = source.split(":");

			PrintStream out = new PrintStream(client.getOutputStream());

			out.println(reply);
			out.close();
			// socket.close();
		}
	}

	private void processSearchResult(CANMessage message) {
		if (message.requestType.equals("found")) {
			System.out.println("File found at : " + message.messageFrom);

			// Print path to file.
			System.out.println();
			System.out.println("Search route to file:");
			for (String node : message.path) {
				System.out.println(node);
			}
		} else
			System.out.println("Failure.");
	}

	private void uploadFile(CANMessage message) throws UnknownHostException,
			IOException, InterruptedException {
		String target = message.messageFrom;
		int index = target.indexOf(" ");
		target = target.substring(index + 1);
		// System.out.println("File target is: " + message.messageFrom);

		String host[] = target.split(":");
		Socket s = new Socket(host[0], Integer.parseInt(host[1]));

		CANMessage backupMessage = new CANMessage("backup", message.message, "");

		ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
		sendMessageToNode(target, backupMessage, os);

		if (message.message == null)
			System.out.println("File null !!");
		else {
			// System.out.println(message.file);

			// File name is in message objects message field inserted in method
			// service message "backuptarget" case of if-else structure.
			File file = new File(Peer.getDir() + "/" + message.message);
			
			streamFile(file, os, s);
		}
	}

	// Streams file over out stream over open socket.
	public void streamFile(File file, ObjectOutputStream outStream, Socket s) {
		try{
		byte[] buffer = new byte[1024];
		int count;
		OutputStream out = s.getOutputStream();

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				file));
		// System.out.println("sending " + file);
		while ((count = in.read(buffer)) != -1) {
			out.write(buffer, 0, count);
			out.flush();
		}
		// System.out.println("File sent");
		// file.delete();
		s.close();
		}catch(Exception exp){
//			System.out.println("stream :" + exp.getMessage());
		}
	}

	public void sendMessageToNode(String address, CANMessage message,
			ObjectOutputStream out) throws NumberFormatException,
			UnknownHostException, IOException, InterruptedException {
		String host[] = address.split(":");

		Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

		out.writeObject(message);
	}

	static void copy(InputStream in, OutputStream out) throws IOException {
		System.out.println("In copy");
		byte[] buf = new byte[8192];
		int len = 0;
		while ((len = in.read(buf)) != -1) {
			System.out.println("writing" + len + " bytes");
			out.write(buf, 0, len);
			System.out.println("Wrote");
		}

		System.out.println("Exiting copy");
	}

	@SuppressWarnings("unchecked")
	private void performMerge(CANMessage message) {

		// Get all files
		Peer.files
				.putAll((Map<? extends String, ? extends String>) message.files);

		// Merge zones.
		Peer.peerZone.merge((Zone) message.newZone);

		mergeNeighbors(message, "regular");

	}

	private void performTempMerge(CANMessage message) {
		// Get all files
		Peer.files
				.putAll((Map<? extends String, ? extends String>) message.files);

		Peer.tempZone = (Zone) message.newZone;

		mergeNeighbors(message, "temp");
	}

	private void mergeNeighbors(CANMessage message, String type) {

		ArrayList<ArrayList<String>> receivedNeighbors = (ArrayList<ArrayList<String>>) message.neighbors;

		ListIterator<ArrayList<String>> directionIterator = receivedNeighbors
				.listIterator();

		int dir = 0;
		while (directionIterator.hasNext()) {

			ArrayList<String> direction = directionIterator.next();
			ListIterator<String> neighborIterator = direction.listIterator();

			while (neighborIterator.hasNext()) {
				String str = new String(neighborIterator.next());

				// Ignoring already existing neighbors.
				if (!Peer.neighbours.get(dir).contains(str)) {

					// Ignoring self.
					if (!str.equalsIgnoreCase(Peer.CANid + ":" + Peer.hostName)) {
						Peer.neighbours.get(dir).add(str);
					}
				}
			}
			dir++;

			if (type.equalsIgnoreCase("regular"))
				Peer.informNeighborsOfExistence();
			else
				Peer.informNeighborsOfExistenceTemp();
		}
	}

	private void getBorderAndAreaStats(CANMessage message) {

		boolean sameArea = false;
		boolean completeOverLap = false;

		if (message.zone.Area() == Peer.peerZone.Area())
			sameArea = true;

		if (Peer.peerZone.isEdgeCompleteOverlap(message.zone))
			completeOverLap = true;

		try {
			PrintStream out = new PrintStream(client.getOutputStream());

			out.println(sameArea);
			out.println(completeOverLap);
			out.close();

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	public void checkForFile(CANMessage message) throws IOException {

		boolean found = checkIfFilePresentInYourDir(message.file);

		// System.out.println("Replying to " + message.source);

		String host[] = message.source.split(":");

		Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

		OutputStream out = socket.getOutputStream();
		PrintStream ps = new PrintStream(out);
		if (found)
			ps.println(Peer.CANid);
		else
			ps.println("notfound");

		socket.close();
	}

	public boolean checkIfFilePresentInYourDir(String file) {

		// Get full path name for file and return true if exists.
		String path = Peer.getDir()  + Peer.hostName + "/" + file;
		// path = "/home/mandeep/got.pdf";

		File filePath = new File(path);

		return filePath.exists();
	}

	private void insertFile(CANMessage message) {
		Peer.files.put(message.file, message.file);

		try {

			CANMessage confirm = new CANMessage("inserted", "File inserted at "
					+ Peer.hostName, Peer.CANid);

			sendMessageTo(message.source, confirm);

		} catch (NumberFormatException e) {
			System.out.println(e.getMessage());
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	private void getJoinDetails(CANMessage message) {
		Peer.peerZone = (Zone) message.newZone;
		Peer.neighbours = (ArrayList<ArrayList<String>>) message.neighbors;
		Peer.files = (HashMap<String, String>) message.files;

		Peer.isInCAN = true;
		Peer.informNeighborsOfExistence();

	}
	
	public void myMethod(CANMessage message) throws NumberFormatException, ClassNotFoundException, IOException, InterruptedException{
		processMessage(message);
	}

	public void processMessage(CANMessage message) throws IOException,
			ClassNotFoundException, NumberFormatException, InterruptedException {

		if (message.requestType.equals("update")
				|| message.requestType.equals("delete")
				|| message.requestType.equals("welcome")
				|| message.requestType.equals("distance")
				|| message.requestType.equals("inserted")
				|| message.requestType.equals("found")
				|| message.requestType.equals("foundtarget")
				|| message.requestType.equals("notfound")
				|| message.requestType.equals("border")
				|| message.requestType.equals("merge")
				|| message.requestType.equals("area")
				|| message.requestType.equals("backup")
				|| message.requestType.equals("sendfile")
				|| message.requestType.equals("tempmerge")
				|| message.requestType.equals("backup")) {
			serviceMessage(message);
		} else if (Peer.peerZone.isPointInZone(message.destination)) {
			serviceMessage(message);
		} else {
			greedyRoute(message);
		}

	}

	public void run() {
		ObjectInputStream ipReader = null;
		try {
			if (client == null)
				System.out
						.println("Error initializing thread client for Peer.");
			else {

				ipReader = new ObjectInputStream(client.getInputStream());

				// read CAN message from peer.
				CANMessage message = (CANMessage) ipReader.readObject();

				processMessage(message);
			}

		} catch (EOFException eof) {
//			System.out.println("eof");
		} catch (Exception exp) {
//			System.out.println("wrun:" + exp.getMessage());;
		}
	}

	public static void main(String args[]) throws IOException {
		int port = Utilities.getAvailablePort();
		ServerSocket s = new ServerSocket(port);
		PeerWorker w = new PeerWorker();
		CANMessage m = new CANMessage(new ArrayList<String>(), "check", s
				.getInetAddress().toString(), new Point(3., 4.0), "got.pdf");
		w.checkForFile(m);
	}
}