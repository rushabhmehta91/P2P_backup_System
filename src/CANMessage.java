import java.io.Serializable;
import java.util.ArrayList;

public class CANMessage implements Serializable {

	int port, side;
	Point destination;
	Zone zone;
	Object newZone, neighbors, files;
	String file, messageFrom, message, requestType, source, destinationPeer;
	String fileToUpload;
	ArrayList<String> path;
	int sendToPort;
	
	
	boolean edgeOverlap, sameArea;
	
	public CANMessage(String requestType){
		this.requestType = requestType;
	}

	public CANMessage(String requestType, String source, Point destination) {
		this.requestType = requestType;
		this.source = source;
		this.destination = destination;
	}
	
	public CANMessage(String requestType, Point destination, String message){
		this.requestType = requestType;
		this.destination = destination;
		this.message = message;
	}
	
	
	
	public CANMessage(boolean edgeOverlap, boolean sameArea){
		this.edgeOverlap = edgeOverlap;
		this.sameArea = sameArea;
	}
	
	// For leave probe.
	public CANMessage(String requestType, Zone zone){
		this.requestType = requestType;
		this.zone = zone;
	}
	public CANMessage(ArrayList<String> path, String requestType, String source, Point destination,
			String filename){
		this.path = path;
		this.requestType = requestType;
		this.source = source;
		this.destination = destination;
		this.file = filename;
		
	}
	
	public CANMessage(ArrayList<String> path,String requestType, String message, String messageFrom){
		this.requestType = requestType;
		this.message = message;
		this.messageFrom = messageFrom;
		this.path = path;
	}
	
	public CANMessage(String requestType, String message, String messageFrom){
		this.requestType = requestType;
		this.message = message;
		this.messageFrom = messageFrom;
	}

	public CANMessage(String requestType, String source, Point destination,
			String filename){
		this.requestType = requestType;
		this.source = source;
		this.destination = destination;
		this.file = filename;
		
	}

	public CANMessage(String requestType, String source, int side, Zone zone) {
		this.requestType = requestType;
		this.source = source;
		this.side = side;
		this.zone = zone;
		// this.destinationPeer = destination;
	}

	public CANMessage(String requestType, Object newZone, Object neighbors, Object files) {
		this.requestType = requestType;
		this.newZone = newZone;
		this.neighbors = neighbors;
		this.files = files;
	}
	public CANMessage(String requestType, String fileName, int sendToPort) {
		this.requestType = requestType;
		this.file = fileName;
		this.sendToPort = sendToPort;
	}

	public static Point getHashFileName(String filename) {
		int odd = 0, even = 0;

		// Calculate hash for filename.
		for (int i = 1; i < filename.length(); i += 2) {
			odd += filename.charAt(i);
		}
		for (int i = 0; i < filename.length(); i += 2) {
			even += filename.charAt(i);
		}
		// Get point where file will be inserted
		Point insertionPoint = new Point((odd+0.1) % 10, (even+0.1) % 10);
		return insertionPoint;
	}
	
	public static void main(String[] args) {
		System.out.println(getHashFileName("nexus5").toString());

	}

}
