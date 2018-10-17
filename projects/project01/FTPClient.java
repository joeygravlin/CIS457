import java.io.*;
import java.nio.*;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;
// import java.nio.file.Path;
import java.net.*;
import java.util.*;

import java.text.
    *;import java.lang.*;
import javax.swing.*;

class FTPClient {
    final static String CRLF = "\r\n";

    public static String getUTF8String(InputStream in) throws IOException {
        Reader r = new InputStreamReader(in, "UTF-8");
        r = new BufferedReader(r, 1024);
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = r.read()) != -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    public static String getTokensToString(StringTokenizer tokens) {
        StringBuilder sb = new StringBuilder();
        sb.append(tokens.nextToken());
        while (tokens.hasMoreTokens()) {
            sb.append(" " + tokens.nextToken());
        }
        return sb.toString();
    }

    public static void readFileToDataOutputStream(Path filename, DataOutputStream dataOutputStream) {
        try (InputStream in = Files.newInputStream(filename);
                BufferedReader reader =
                new BufferedReader(new InputStreamReader(in))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    dataOutputStream.writeBytes(line);
                    System.out.println(line);
                }
            } catch (IOException x) {
                System.err.println(x);
            }
    }

    public static void main(String argv[]) throws Exception {
        String sentence;
        String modifiedSentence;
        boolean isOpen = true;
        int number = 1;

        Socket controlSocket;
        DataOutputStream outToServer;
        DataInputStream inFromServer;
        int controlPort, dataPort;
        // The root directory for the ftp server.
        Path ftpRootDir = Paths.get("./ftp_client_root_dir");

        boolean notEnd = true;
        String statusCode;
        boolean clientgo = true;

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        sentence = inFromUser.readLine();
        StringTokenizer tokens = new StringTokenizer(sentence);

        if (sentence.startsWith("CONNECT")) {
            String serverName = tokens.nextToken(); // pass the connect command
            serverName = tokens.nextToken();
            controlPort = Integer.parseInt(tokens.nextToken());
            dataPort = controlPort + 1;

            System.out.println("You are connecting to " + serverName);
            System.out.println("On dataPort:  " + controlPort);

            controlSocket = new Socket(serverName, controlPort);
            // try {
            //     controlSocket = new Socket(serverName, controlPort);
            // } catch (Exception e) {
            //     System.out.println(e);
            // }

            System.out.println("You are connected to " + serverName + ":" + controlPort);

            while (isOpen && clientgo) {

                outToServer = new DataOutputStream(controlSocket.getOutputStream());

                inFromServer = new DataInputStream(new BufferedInputStream(controlSocket.getInputStream()));

                sentence = inFromUser.readLine();

                if (sentence.equals("LIST")) {
                    dataPort = dataPort + 2;
                    System.out.println("writing command to server: " + dataPort + " " + sentence);
                    outToServer.writeBytes(dataPort + " " + sentence + " " + CRLF);

                    ServerSocket server = new ServerSocket(dataPort);
                    Socket dataSocket = server.accept();

                    try (DataInputStream inData = new DataInputStream(
                                new BufferedInputStream(dataSocket.getInputStream()))) {

                        modifiedSentence = getUTF8String(inData);
                        System.out.println(modifiedSentence);

                    } catch (IOException e) {
                        // Output unexpected IOExceptions.
                        System.out.println(e);
                    }

                    dataSocket.close();
                    server.close();
                    System.out.println("datasocket is closed");
                    System.out.println("\nWhat would you like to do next:\n retr: name.txt ||stor: file.txt  || close");

                } else if (sentence.startsWith("RETR ")) {
                    dataPort = dataPort + 2;

                    System.out.println("writing command to server:\n" + dataPort + " " + sentence);
                    outToServer.writeBytes(dataPort + " " + sentence + " " + CRLF);

                    StringTokenizer commandTokens = new StringTokenizer(sentence);
                    String clientCommand = commandTokens.nextToken();
                    String commandTarget = "";
                    if (commandTokens.hasMoreTokens()) {
                        commandTarget = FTPClient.getTokensToString(commandTokens);
                    }

                    ServerSocket server = new ServerSocket(dataPort);
                    Socket dataSocket = server.accept();

                    try (DataInputStream inData = new DataInputStream(
                                new BufferedInputStream(dataSocket.getInputStream()))) {


                        // StringBuffer sb = new StringBuffer();
                        // try {
                        //     while (inData.available() > 0) {
                        //         sb.append(inData.readUTF());
                        //     }
                        // } catch (Exception e) {
                        //     System.err.println(e);
                        // }
                        modifiedSentence = getUTF8String(inData);
                        // System.out.println(modifiedSentence);

                        // Convert the string to a byte array.
                        // byte data[] = sb.toString().getBytes();
                        byte data[] = modifiedSentence.getBytes();
                        Path p = Paths.get(ftpRootDir + "/" + commandTarget);

                        try (OutputStream out = new BufferedOutputStream(
                            Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) {
                            out.write(data, 0, data.length);
                            System.out.println("File: '" + commandTarget + "' written.");
                        } catch (IOException x) {
                            System.err.println(x);
                        }

                    } catch (IOException e) {
                        // Output unexpected IOExceptions.
                        System.out.println(e);
                    }

                    dataSocket.close();
                    server.close();
                    System.out.println("datasocket is closed");
                } else if (sentence.startsWith("STOR ")) {
                    dataPort = dataPort + 2;
                    outToServer.writeBytes(dataPort + " " + sentence + " " + CRLF);

                    ServerSocket server = new ServerSocket(dataPort);
                    Socket dataSocket = server.accept();
                    DataInputStream inData = new DataInputStream(new BufferedInputStream(dataSocket.getInputStream()));

                    // ......................
                    dataSocket.close();
                } else if (sentence.equals("QUIT")) {
                    isOpen = false;
                    clientgo = false;
                    outToServer.writeBytes(dataPort + " " + sentence + " " + CRLF);
                    System.out.println("Client is disconnecting");
                    controlSocket.close();
                } else {
                    System.out.println("\nCommand not recognized\n");
                }
            }
        } else {
            System.out.println("\nCommand not recognized:\nPlease CONNECT to a server first.");
        }
    }
}
