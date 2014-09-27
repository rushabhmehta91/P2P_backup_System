import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer {

    public static Zone peerZone;
    public static Zone tempZone;
    public static ArrayList<ArrayList<String>> neighbours = new ArrayList<ArrayList<String>>();
    public static HashMap<String, String> files = new HashMap<>();

    public static String bootStrap;
    public static int bootstrapPort;

    public static int portforCAN;
    public static String ip = null;
    public static String CANid = null;
    public static String hostName = null;
    public static String userName = null;
    public static String password = null;
    public static boolean oldUser = false;

    public static String getDir() {
        return "/home/stu12/s11/mhs1841/Documents/";
    }

    public static boolean isInCAN = false;

    Peer(String bootStrap, int port) {
        Peer.bootstrapPort = port;
        Peer.bootStrap = bootStrap;

        for (int i = 0; i < 4; i++) {
            neighbours.add(new ArrayList<String>());
        }

        try {
            ip = InetAddress.getLocalHost().toString().split("/")[1];
            hostName = InetAddress.getLocalHost().toString().split("/")[0];
        } catch (UnknownHostException e) {

            System.out.println(e.getMessage());
        }

    }

    private void sendJoinRequest(String response) throws UnknownHostException {

        Point destination = getRandomPoint();

        String ip = InetAddress.getLocalHost().toString().split("/")[1];

        CANMessage message = new CANMessage("join", ip + ":"
                + String.valueOf(Peer.portforCAN) + ":" + hostName, destination);

        String host[] = response.split(":");

        try {
            // Create socket to entry point and specific port.
            Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

            ObjectOutputStream output = new ObjectOutputStream(
                    socket.getOutputStream());

            // Send out the join message.
            output.writeObject(message);

        } catch (Exception exp) {
            System.out.println(exp.getMessage());
        }

    }

    private String getRandomNeighbor() {
        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = neighborIterator.next();
                return neighbor;
            }
        }

        return null;
    }

    private void giveBootStrapAlternateEntryPoint() {

        Socket socket = null;
        PrintStream out = null;
        try {
            socket = new Socket(bootStrap, bootstrapPort);

            out = new PrintStream(socket.getOutputStream());
            String nnn = getRandomNeighbor();
            // System.out.println(nnn);
            out.println(nnn);
            out.println(nnn);

            out.close();
            socket.close();

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            System.out.println(e1.getMessage());
        }
    }

    private void initiateMerge(String merger, String mergerType)
            throws NumberFormatException, UnknownHostException, IOException {

        // Give all my info to peer who is taking over.
        CANMessage mergePackage = new CANMessage("tempmerge", Peer.peerZone,
                Peer.neighbours, Peer.files);

        giveBootStrapAlternateEntryPoint();

        informNeighborsOfDeparture();

        String host[] = merger.split(":");
        Socket socket = new Socket(host[0], Integer.parseInt(host[1]));
        ObjectOutputStream out = new ObjectOutputStream(
                socket.getOutputStream());

        out.writeObject(mergePackage);
        out.close();

        System.out.println("Sent out irregular merge request to  " + merger);
        socket.close();

        Peer.files.clear();
        Peer.peerZone = null;
        Peer.neighbours = null;
        Peer.isInCAN = false;
        System.out.println();
        System.out.println("Leaving CAN. Restart program to rejoin.");

        System.exit(0);

    }

    private void initiateMerge(String merger) throws NumberFormatException,
            UnknownHostException, IOException {

        // Give all my info to peer who is taking over.
        CANMessage mergePackage = new CANMessage("merge", Peer.peerZone,
                Peer.neighbours, Peer.files);
        giveBootStrapAlternateEntryPoint();
        informNeighborsOfDeparture();
        String host[] = merger.split(":");
        Socket socket = new Socket(host[0], Integer.parseInt(host[1]));
        ObjectOutputStream out = new ObjectOutputStream(
                socket.getOutputStream());
        out.writeObject(mergePackage);
        out.close();
        socket.close();

        Peer.files.clear();
        Peer.peerZone = null;
        Peer.neighbours = null;

        Peer.isInCAN = false;

        System.out.println();
        System.out.println("Leaving CAN. Restart program to rejoin.");

        System.exit(0);
    }

    private String getSmallestNeighbor() {

        double min = Double.MAX_VALUE;
        String smallestPeer = null;
        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = neighborIterator.next();
                String host[] = neighbor.split(":");

                try {
                    Socket socket = new Socket(host[0],
                            Integer.parseInt(host[1]));
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream());
                    InputStreamReader ireader = new InputStreamReader(
                            socket.getInputStream());
                    BufferedReader in = new BufferedReader(ireader);
                    CANMessage areaProbe = new CANMessage("area");

                    out.writeObject(areaProbe);

                    double area = Double.parseDouble(in.readLine());

                    if (area <= min) {
                        smallestPeer = new String(neighbor);
                        min = area;
                    }

                    in.close();
                    out.close();
                    socket.close();

                } catch (Exception exp) {
                    System.out.println(exp.getMessage());
                }
            }
        }
        return smallestPeer;
    }

    private void attemptIrregularMerge() throws NumberFormatException,
            UnknownHostException, IOException {
        String smallest = getSmallestNeighbor();

        if (smallest == null) {
            System.out.println();
            System.out.println("Leaving CAN. Restart program to rejoin CAN");
            System.exit(0);
        }
        System.out.println("Smallest neighbor is : " + smallest);
        initiateMerge(smallest, "temp");
    }

    private void informNeighborsOfDeparture() {

        int i = 0;

        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = neighborIterator.next();
                String host[] = neighbor.split(":");

                try {
                    // Contact neighbor with delete message.
                    Socket socket = new Socket(host[0],
                            Integer.parseInt(host[1]));
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream());
                    InputStreamReader ireader = new InputStreamReader(
                            socket.getInputStream());
                    BufferedReader in = new BufferedReader(ireader);

                    CANMessage message = new CANMessage("delete", Peer.CANid
                            + ":" + Peer.hostName, (i + 2) % 4, Peer.peerZone);
                    out.writeObject(message);
                } catch (ConnectException exp) {
                    System.out.println("Unable to connect to " + neighbor);
                } catch (Exception exp) {
                    System.out.println(exp.getMessage());
                }
            }
            // Go up a direction
            i++;
        }

    }

    public static synchronized void informNeighborsOfExistenceTemp() {

        int i = 0;

        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = new String(neighborIterator.next());
                String host[] = neighbor.split(":");

                try {
                    Socket socket = new Socket(host[0],
                            Integer.parseInt(host[1]));
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream());
                    InputStreamReader ireader = new InputStreamReader(
                            socket.getInputStream());
                    BufferedReader in = new BufferedReader(ireader);

                    CANMessage message = new CANMessage("update", Peer.CANid
                            + ":" + Peer.hostName, (i + 2) % 4, Peer.tempZone);
                    out.writeObject(message);

                    String isNeighbor = in.readLine();

                    if (isNeighbor != null && isNeighbor.equals("no")) {

                        neighborIterator.remove();
                    }

                    in.close();
                    out.close();
                    socket.close();
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            i++;
        }

    }

    public static synchronized void informNeighborsOfExistence() {

        int i = 0;

        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = new String(neighborIterator.next());
                String host[] = neighbor.split(":");

                try {
                    Socket socket = new Socket(host[0],
                            Integer.parseInt(host[1]));
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream());
                    InputStreamReader ireader = new InputStreamReader(
                            socket.getInputStream());
                    BufferedReader in = new BufferedReader(ireader);

                    CANMessage message = new CANMessage("update", Peer.CANid
                            + ":" + Peer.hostName, (i + 2) % 4, Peer.peerZone);
                    out.writeObject(message);

                    String isNeighbor = in.readLine();

                    // Removing peers that are not neighbors anymore.
                    if (isNeighbor != null && isNeighbor.equals("no")) {

                        neighborIterator.remove();
                    }

                    in.close();
                    out.close();
                    socket.close();

                } catch (ConnectException exp) {
                    System.out.println("Unable to connect to " + neighbor);
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                } catch (UnknownHostException e) {

                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    System.out.println(e.getMessage());
                }

            }
            i++;

        }
    }

    private String getMergeCandidate() {
        ArrayList<String> secondBest = new ArrayList<String>();
        ListIterator<ArrayList<String>> directionIterator = Peer.neighbours
                .listIterator();
        while (directionIterator.hasNext()) {

            ArrayList<String> direction = directionIterator.next();
            ListIterator<String> neighborIterator = direction.listIterator();

            while (neighborIterator.hasNext()) {

                String neighbor = neighborIterator.next();
                String host[] = neighbor.split(":");

                try {
                    Socket socket = new Socket(host[0],
                            Integer.parseInt(host[1]));
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream());
                    InputStreamReader ireader = new InputStreamReader(
                            socket.getInputStream());
                    BufferedReader in = new BufferedReader(ireader);

                    CANMessage probe = new CANMessage("border", Peer.peerZone);

                    out.writeObject(probe);

                    boolean sameArea = Boolean.parseBoolean(in.readLine());
                    boolean overlap = Boolean.parseBoolean(in.readLine());

                    // Add to second choice list if edges at least overlap.
                    if (overlap)
                        secondBest.add(new String(neighbor));

                    if (sameArea && overlap)
                        return neighbor;

                } catch (Exception exp) {
                    System.out.println(exp.getMessage());
                }
            }

        }

        // Return random second choice peers for take over.
        if (!secondBest.isEmpty()) {
            Random r = new Random();
            int random = r.nextInt(secondBest.size());
            return secondBest.get(random);
        }

        return null;

    }

    private Point getRandomPoint() {
        Random r = new Random();
        int x = r.nextInt(10);
        int y = r.nextInt(10);

        Point destination = new Point(x, y);

        return destination;
    }

    private void joinCAN() throws IOException, BindException, Exception {

        // Get port peer is dedicating to CAN
        int port = getPortForCAN();

        Socket socketToServer = new Socket();
        try {
            socketToServer.connect(new InetSocketAddress(bootStrap,
                    Peer.bootstrapPort));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            InputStreamReader ipreader = new InputStreamReader(
                    socketToServer.getInputStream());
            BufferedReader input = new BufferedReader(ipreader);
            PrintStream output = new PrintStream(
                    socketToServer.getOutputStream());

            output.println("authenticate");
            login(input, output);
            // System.out.println("sending join");
            output.println("join");
            output.println(portforCAN);

            // Wait for response.
            String response = input.readLine();

            // First peer in CAN
            if (response.equals("owner")) {
                // Assign full space.
                Peer.peerZone = new Zone(new Point(0.0, 0.0), new Point(10.0,
                        10.0));
                isInCAN = true;
                viewDetails();
                startListeningInCAN();
                informNeighborsOfExistence();
                // System.out.println("Done");

            } else if (response.equals("unrecognizedcommand"))
                System.out.println("Server did not recognize command");
            else {
                sendJoinRequest(response);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        // System.out.println(isInCAN);
        // while(!isInCAN);
        // System.out.println(isInCAN);
        // System.out.println("HEEEEEEEEEEEERRRRRREEEE" + oldUser);
        // if (oldUser) {
        // System.out.println("Retreiving file list");
        // getFile(userName + "_list.txt");
        // System.out.println("Got file");
        // moveFile(getDir() + hostName + "/ret/" + userName + "_list.txt");
        // // new File(getDir()+hostName+"/ret/",userName+"_list.txt");
        // System.out.println("do you want to view list of file you uploaded");
        // Scanner sc = new Scanner(System.in);
        // String reply1 = sc.next();
        // if (reply1.equalsIgnoreCase("y")) {
        // displayUserList();
        // }
        // } else {
        if (!oldUser) {
            // System.out.println("Backing up new file");
            try {
                backupFile(getDir() + userName + "_list.txt");
                // new File(getDir()+hostName+"/ret/",userName+"_list.txt");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // }
        // System.out.println("All done !!!");
    }

    public Boolean authenticateUser(String username, String password,
                                    BufferedReader input, PrintStream output) throws IOException,
            ClassNotFoundException {
        output.println("authenticate");
        output.println(username);
        output.println(password);
        String reply = null;
        try {
            reply = input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (reply.equals("loginsucc")) {
            System.out.println("login succesfull");
            oldUser = true;
            return true;
        } else {
            if (reply.equalsIgnoreCase("wrongpass"))
                return false;
            else {
                System.out.println("Username not found.");
                System.out.println("Sign up?(y/n):");

                Scanner in = new Scanner(System.in);
                String decision = in.next();

                if (decision.equalsIgnoreCase("y")) {
                    signup(input, output);
                    return true;
                } else
                    return false;
            }
        }
    }

    private void moveFile(String path) throws IOException {
        File oldFile = new File(path);
        File newFile = new File(getDir() + userName + "_list.txt");
        InputStream in = new FileInputStream(oldFile);
        OutputStream out = new FileOutputStream(newFile);
        byte[] buffer = new byte[1024];
        int readbuff;
        while ((readbuff = in.read(buffer)) > 0) {
            out.write(buffer, 0, readbuff);
        }
        in.close();
        out.close();
        oldFile.delete();
    }

    private void displayUserList() throws Exception {

        // System.out.println("Retreiving file list");
        getFile(userName + "_list.txt");
        // System.out.println("Got file");
        File f = new File(getDir() + hostName + "/ret/" + userName
                + "_list.txt");
        if (f.exists()) {
            moveFile(getDir() + hostName + "/ret/" + userName + "_list.txt");
            String path = getDir() + userName + "_list.txt";

            try {
                FileReader fc = new FileReader(path);
                BufferedReader br = new BufferedReader(fc);
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (FileNotFoundException e) {
                System.out.println("file not found");
            }
        }
    }

    private void signup(BufferedReader input, PrintStream output)
            throws IOException {

        output.println("signup");
        output.println(userName);
        output.println(password);

        String reply = input.readLine();

        if (reply.equalsIgnoreCase("signupsucc"))
            try {
                createUserFileList();
            } catch (Exception e) {
                System.out.println("Error in creating file filelist");
                // e.getStackTrace();
            }

    }

    private void createUserFileList() {
        String userlistfilename = getDir() + userName + "_list.txt";
        try {
            PrintWriter writer = new PrintWriter(userlistfilename, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void login(BufferedReader input, PrintStream output) {
        try {
            boolean flag = true;
            do {
                if (!flag) {
                    System.out.println("Invalid Username/password");
                }

                System.out.print("Enter User Name: ");
                Scanner sc = new Scanner(System.in);
                userName = sc.next();

                Console c = System.console();
                char[] passArray = c.readPassword("Enter password: ");
                password = new String(passArray);
                flag = authenticateUser(userName, password, input, output);
            } while (!flag);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void startListeningInCAN() throws IOException {

        try {
            File dir = new File("/home/stu12/s11/mhs1841/Documents/"
                    + Peer.hostName);
            if (!dir.exists())
                dir.mkdir();
            File ret = new File(dir.getAbsoluteFile() + "/ret");
            if (!ret.exists())
                ret.mkdir();

        } catch (Exception exp) {
            exp.printStackTrace();
        }
        // System.out.println("Listening at port: " + portforCAN);
        new Thread(new PeerBackground(Peer.portforCAN)).start();
    }

    private int getPortForCAN() throws BindException, IOException {
        System.out.print("Dedicated port : ");
        Scanner in = new Scanner(System.in);
        int port = in.nextInt();
        // System.out.println("Port returned : " + port);
        Peer.portforCAN = port;

        Peer.CANid = ip + ":" + String.valueOf(Peer.portforCAN);

        startListeningInCAN();
        return port;
    }

    public static void viewDetails() {
        System.out.println();
        try {
            System.out.print("Peer:	"
                    + InetAddress.getLocalHost().getCanonicalHostName());
            System.out.println(" @ " + Peer.CANid);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
        // System.out.println("Zone:	" + Peer.peerZone.toString());

        // if (tempZone != null)
        // System.out.println("Zone:	" + Peer.tempZone.toString());
        // // Printing neighbor details
        // System.out.println("Neighbors:");
        // int count = 0;
        // for (ArrayList<String> direction : neighbours) {
        // if (!direction.isEmpty()) {
        // if (count == 0)
        // System.out.print("	Left:   ");
        // else if (count == 1)
        // System.out.print("	Top:    ");
        // else if (count == 2)
        // System.out.print("	Right:  ");
        // else
        // System.out.print("	Bottom: ");
        // System.out.println(direction);
        // }
        // count++;
        // }

        // System.out.println("Files:");
        // System.out.print("	");
        // for (String filename : files.keySet()) {
        // System.out.print(filename + " ");
        // }
        System.out.println();
    }

    private static void search(String filename) {
        Point locationOfFile = getHashFileName(filename);

        if (peerZone.isPointInZone(locationOfFile)) {
            if (files.keySet().contains(filename))
                System.out.println("File is here with you at " + hostName + ":"
                        + CANid);
            else
                System.out.println("Failure!");
        }

        // else release into CAN.
        else {
            PeerWorker worker = new PeerWorker();
            ArrayList<String> path = new ArrayList<String>();
            CANMessage search = new CANMessage(path, "search", CANid,
                    locationOfFile, filename);
            worker.greedyRoute(search);
        }
    }

    private void insertFile(String filename) {

        Point insertionPoint = getHashFileName(filename);

        // Check if destination is in own zone and insert.
        if (peerZone.isPointInZone(insertionPoint)) {
            files.put(filename, filename);
            System.out.println("File inserted here :" + Peer.CANid + ":"
                    + Peer.hostName);
        }

        // else release into CAN.
        else {
            PeerWorker worker = new PeerWorker();
            CANMessage insert = new CANMessage("insert", Peer.CANid,
                    insertionPoint, filename);
            worker.greedyRoute(insert);
        }
    }

    public void backupFile(String filename) throws Exception {

        File checkIfFileExists = new File(filename);
        if (!checkIfFileExists.exists())
            System.out.println("File not found.");
        String oldName = filename;
        Utilities.encrypt(filename, Peer.userName, Peer.password);
        // Split encrypted file
        filename = filename + ".enc";
        Utilities.splitFile(filename, 2);
        File encrypt = new File(filename);
        for (int part = 1; part <= 2; ++part) {
            // Get point where file is supposed to go based on hashes.
            File file = new File(filename + ".00" + part);
            String firstHash = Utilities.sha1(file.getName());

            // first Copy
            sendToTarget(file, firstHash);

            // second copy
            String secondHash = Utilities.sha1(firstHash);
            sendToTarget(file, secondHash);
        }
        File f = new File(oldName);

        // Write backed up file to file list.
        String path = getDir() + userName + "_list.txt";
        if (!f.getName().contains("_list")) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(
                    new FileWriter(path, true)))) {
                out.println(f.getName());

            } catch (IOException e) {
                // exception handling left as an exercise for the reader
            }
        }

        backupUpdatedFileList(path);


    }

    private void backupUpdatedFileList(String filename) throws Exception {
        File checkIfFileExists = new File(filename);
        if (!checkIfFileExists.exists())
            System.out.println("File not found.");
        String oldName = filename;
        Utilities.encrypt(filename, Peer.userName, Peer.password);
        // Split encrypted file
        filename = filename + ".enc";
        Utilities.splitFile(filename, 2);
        File encrypt = new File(filename);
        for (int part = 1; part <= 2; ++part) {
            // Get point where file is supposed to go based on hashes.
            File file = new File(filename + ".00" + part);
            String firstHash = Utilities.sha1(file.getName());

            // first Copy
            sendToTarget(file, firstHash);

            // second copy
            String secondHash = Utilities.sha1(firstHash);
            sendToTarget(file, secondHash);

        }

    }

    public void getFile(String filename) throws Exception {
        // System.out.println("Retreiving " + filename);
        String part1At, part2At;
        String originalFileName = filename;
        String newFileName = filename + ".dec";
        // Looking for .enc files at remote location.
        filename = filename + ".enc";
        part1At = doPart(filename, 1);

        // System.out.println("Found part 1 @" + part1At);
        // Find part 1.
        if (part1At == null) {
            System.out
                    .println("Part 1 replicas not found. File cannot be recovered.");
            return;
        }

        // Find part 2
        part2At = doPart(filename, 2);
        // System.out.println("Found part 2 @" + part1At);
        if (part2At == null) {
            System.out
                    .println("Part 2 replicas not found. File cannot be recovered.");
            return;
        }

        // Both parts found, download parallelly.
        parallelDownload(part1At, part2At, filename);

		/*
         * Join fragments. Arguments: filename = path of fragments...directory
		 * path ending with /ret/ 2nd argument is name of fragments. 3rd
		 * argument is number of parts. 4th arg is name of file to be stored
		 * after decryption
		 */
        String pathOfFragments = getDir() + hostName + "/ret/";
        System.out.print("Joining fragments... ");
        Utilities.join(pathOfFragments, filename, 2, newFileName);
        System.out.println("complete.");
        System.out.print("Decrypting...");
        Utilities.decrypt(pathOfFragments + newFileName, Peer.userName,
                Peer.password, originalFileName);
        System.out.println("complete.");
    }

    private void parallelDownload(String part1At, String part2At,
                                  String filename) throws InterruptedException {

        ExecutorService es = Executors.newCachedThreadPool();
		/*
		 * Args to parallel downloader constructor: 1st = name of file as stored
		 * in remote loccation 2nd = Address of remote location 3rd = File name
		 * for downloaded file. Join using this file name
		 */
        ParallelDownloader p1 = new ParallelDownloader(filename + ".001",
                part1At, filename + ".001");
        ParallelDownloader p2 = new ParallelDownloader(filename + ".002",
                part2At, filename + ".002");
        es.execute(p1);
        es.execute(p2);

        boolean finsihed = es.awaitTermination(4, TimeUnit.SECONDS);
        // System.out.println("Fragments downloaded.");

    }

    private String doPart(String filename, int partNo)
            throws NoSuchAlgorithmException, IOException,
            NumberFormatException, ClassNotFoundException, InterruptedException {
        String part1At = null;

        // Compute file name for part.
        String id = filename + ".00" + String.valueOf(partNo);
        for (int replica = 1; replica <= 2; ++replica) {

            // Get hash for replica 1 and compute location.
            String hash = Utilities.sha1(id);
            Point locationOfPart_1 = getHashFileName(hash);

            // System.out.println(locationOfPart_1);

            // Get open socket to receive replies.
            int port = Utilities.getAvailablePort();
            ServerSocket s = new ServerSocket(port);
            String source = ip + ":" + String.valueOf(port);

			/*
			 * Message containing: "check" command source = this/requesting
			 * peers ID. filename with appropriate part number.
			 */

            CANMessage m = new CANMessage(new ArrayList<String>(), "check",
                    source, locationOfPart_1, filename + ".00"
                    + String.valueOf(partNo));

            PeerWorker w = new PeerWorker();
            w.myMethod(m);

            // System.out.println("Wating for reply");
            // Wait for reply.
            Socket reply = s.accept();
            // System.out.println("gotreply");
            InputStream stream = reply.getInputStream();
            InputStreamReader ipReader = new InputStreamReader(stream);
            BufferedReader input = new BufferedReader(ipReader);

            // System.out.println("Reading");
            String found = input.readLine();

            // System.out.println(" Got reply: " + found);
            // File not found, try second copy.
            if (found.equalsIgnoreCase("notfound")) {
                id = hash;
                continue;
            } else {
                part1At = found;
                break;
            }
        }

        // Return address of location where file fragment found.
        return part1At;
    }

    private void sendToTarget(File file, String hash)
            throws NumberFormatException, ClassNotFoundException, IOException,
            InterruptedException {

        Point targetForFirstPiece = getHashFileName(hash);

        // Send message to target coordinates.
        // path is to mimic "search" message because it was easier to extend
        // here.
        ArrayList<String> path = new ArrayList<String>();

        CANMessage findTarget = new CANMessage(path, "backuptarget", CANid,
                targetForFirstPiece, file.getName());
        // PeerWorker worker = new PeerWorker();

        PeerWorker w = new PeerWorker();
        w.myMethod(findTarget);

        // worker.greedyRoute(findTarget);
    }

    /*
     * Direct communication method. Send message without greedy routing to node.
     */
    public static void sendMessageTo(String address, CANMessage message,
                                     ObjectOutputStream out) throws NumberFormatException,
            UnknownHostException, IOException, InterruptedException {

        // Identify remote host.
        String host[] = address.split(":");
        // Create socket.
        Socket socket = new Socket(host[0], Integer.parseInt(host[1]));

        // Write message to stream.
        out.writeObject(message);
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        // System.out.println("In copy");
        byte[] buf = new byte[8192];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            // System.out.println("writing" + len + " bytes");
            out.write(buf, 0, len);
            // System.out.println("Wrote");
        }
        // System.out.println("Exiting copy");
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
        Point insertionPoint = new Point((odd + 0.1) % 10, (even + 0.1) % 10);
        return insertionPoint;
    }

    public void processCommand(String command) throws BindException, Exception {
        StringTokenizer tokens = new StringTokenizer(command);

        if (!tokens.hasMoreTokens())
            throw new Exception("Invalid command");

        String comm = tokens.nextToken();

        if (comm.equalsIgnoreCase("stop")) {
            PeerBackground.stopListening();
        }

        // Check if already in can, else start join process.
        if (comm.equalsIgnoreCase("join")) {
            if (isInCAN)
                throw new Exception("Already in CAN.");

            try {
                joinCAN();
            } catch (BindException exp) {
                throw exp;
            }
            Thread.sleep(1500);
        } else {
            if (!isInCAN)
                throw new Exception("Need to join CAN first.");

            if (comm.equalsIgnoreCase("view")) {
                viewDetails();
            } else if (comm.equalsIgnoreCase("leave")) {
                String merger = getMergeCandidate();
                if (merger == null) {
                    System.out.println("No good candidate for merge found.");
                    System.out.println("Attempting irregular join...");
                    attemptIrregularMerge();

                } else {
                    System.out.println("Asking " + merger + " to take over.");
                    initiateMerge(merger);
                }
            } else if (comm.equalsIgnoreCase("search")) {
                String keyword = tokens.nextToken();
                if (keyword.isEmpty() || keyword == null) {
                    throw new Exception("Keyword missing");
                }
                search(keyword);
                Thread.sleep(1500);
            } else if (comm.equalsIgnoreCase("insert")) {
                String keyword = tokens.nextToken();
                if (keyword.isEmpty() || keyword == null) {
                    throw new Exception("Keyword missing");
                }
                insertFile(keyword);
                Thread.sleep(1500);
            } else if (comm.equalsIgnoreCase("backup")) {
                String file = command.substring(7);
                if (file.isEmpty() || file == null) {
                    throw new Exception("File missing");
                }
                backupFile(file);
            } else if (comm.equalsIgnoreCase("get")) {
                String file = command.substring(4);
                if (file.isEmpty() || file == null) {
                    throw new Exception("File missing");
                }
                getFile(file);
            } else if (comm.equalsIgnoreCase("filelist")) {
                displayUserList();
            } else
                throw new Exception("Invalid command");
        }
    }

    public static void main(String[] args) throws Exception {

        while (true) {
            try {
                String serverName = args[0];
                int port = Integer.parseInt(args[1]);
                // String serverName = "127.0.0.1";
                // int port = 3252;

                Peer thisPeer = new Peer(serverName, port);
                System.out.print("Enter command: ");
                Scanner in = new Scanner(System.in);
                String command = in.nextLine();

                thisPeer.processCommand(command);

            } catch (BindException exp) {
                // System.out.println(exp.getMessage());
                System.exit(0);
            } catch (ArrayIndexOutOfBoundsException exp) {
                System.out
                        .println("Usage: java Peer bootstrap_ip bootstrap_port");
                System.exit(0);
            } catch (Exception e) {
                // e.printStackTrace();
                // System.out.println(e.getMessage());
                continue;

            }

        }

    }
}
