/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/

package assignment7;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;


public class ServerMain /*extends Observable*/{
    //private ArrayList<PrintWriter> clientOutputStreams;
    private ArrayList<Socket> clientSockets;
    private ArrayList<ChatGroup> chatGroups;
    private HashMap<Socket,ClientObserver> sockets;
    private int chatCount = 0;
    private String broadcastUID = "";

    private final int realMsg = 0;
    private final int createChat = 1;
    private final int addToChat = 2;
    private final int leaveChat = 3;
    private final int closeSocket = 4;
    private final int joinBroadcast = 5;

    // NOTE: when user wants to remove themselves, they will send the chatID
    public static void main(String[] args) {
        try {
            new ServerMain().setUpNetworking();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpNetworking() throws Exception {
        chatGroups = new ArrayList<>();
        //clientOutputStreams = new ArrayList<>();
        clientSockets = new ArrayList<>();
        sockets = new HashMap<>();
        @SuppressWarnings("resource")
        ServerSocket serverSock = new ServerSocket(4242);
        while(true) {
            Socket clientSocket = serverSock.accept();
            clientSockets.add(clientSocket);

            //ClientObserver writer = new ClientObserver(clientSocket.getOutputStream());
            //clientOutputStreams.add(writer);
            //addObserver(writer);

            Thread t = new Thread(new ServerMain.ClientHandler(clientSocket));
            t.start();

            System.out.println("got a connection with " + clientSocket.toString());
        }
    }

    private int createChat(Socket clientSocket, String chatUID) throws IOException{
        if(clientSocket != null) {
            ServerMain.ChatGroup chatGroup = new ServerMain.ChatGroup(clientSocket, chatCount, chatUID);
            chatGroups.add(chatGroup);
            chatCount++;
            System.out.println("Existing chat groups: " + chatGroups.size());
            return chatGroup.chatNumber;
        }

        else return -1;
    }

    private boolean addToChat(int port, String chatUID) {
        Socket clientSocket = findClientSocket(port);
        if(clientSocket!=null){
            try{
                for (ServerMain.ChatGroup group: chatGroups) {
                    if (group.chatUID.equals(chatUID)) {
                        // if user is not already a part of this chat group
                        if (!group.clients.containsKey(clientSocket)) {
                            return group.addToChat(clientSocket);
                        }
                        return false;
                    }
                }
            }
            catch (Exception e) { return false; }
        }
        return false;
    }

    // broadcast functionality
    private boolean addToBroadcast(int port, String chatUID){
        Socket clientSocket = findClientSocket(port);
        if(clientSocket!= null){
            try {
                if (chatCount == 0) {
                    ServerMain.ChatGroup chatGroup = new ServerMain.ChatGroup(clientSocket, chatCount, chatUID);
                    broadcastUID = chatUID;
                    chatGroups.add(chatGroup);
                    chatCount++;
                } else {
                    chatGroups.get(0).addToChat(clientSocket);
                }
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        else return false;
    }

    private boolean removeFromChat(Socket clientSocket, String chatUID) {
        boolean returnVal = false;
        for (int i = 0; i < chatGroups.size(); i++) {
            if (chatGroups.get(i).chatUID.equals(chatUID) && !chatUID.equals(broadcastUID)) {
                returnVal = chatGroups.get(i).removeFromChat(clientSocket);
                if (chatGroups.get(i).clients.size() == 0) {
                    chatGroups.remove(chatGroups.get(i));
                    if(chatCount!=0) chatCount--;
                }
            }
        }
        System.out.println("(R)emaining chat groups: " + chatGroups.size());
        return returnVal;
    }

    private void sendMessage(String chatUID, String message) {
        for (ServerMain.ChatGroup group: chatGroups) {
            if (group.chatUID.equals(chatUID)) {
                group.notifyClients(message);
            }
        }
    }

    private Socket findClientSocket (int check_port){
        for (Socket clientSocket : clientSockets){
            int port = clientSocket.getPort();
            if(check_port == port) return clientSocket;
        }
        return null;
    }

//	private void notifyAllClients(String message) {
//		Iterator it = sockets.entrySet().iterator();
//		while (it.hasNext()){
//            Map.Entry entry = (Map.Entry) it.next();
//            ClientObserver writer = (ClientObserver) entry.getValue();
//            writer.send(message);
//        }
//	}


    public class ChatGroup extends Observable{
        private HashMap<Socket, ClientObserver> clients;
        private int chatNumber;
        private String chatUID;

        public ChatGroup(Socket clientSocket, int chatNumber, String uniqueID){
            try {
                clients = new HashMap<>();
                this.chatNumber = chatNumber;
                this.chatUID = uniqueID;
                addToChat(clientSocket);
            } catch (IOException e){
                System.out.println("Couldn't add to chat");
            }
        }

        public boolean addToChat(Socket clientSocket) throws IOException{
            if(clientSocket!=null) {
                ClientObserver writer;
                if (!clients.containsKey(clientSocket)) {
                    writer = new ClientObserver(clientSocket.getOutputStream());
                    sockets.put(clientSocket,writer);
                }
                else {
                    writer = clients.get(clientSocket);
                }
                addObserver(writer);
                clients.put(clientSocket, writer);
                return true;
            }

            else return false;
        }

        public boolean removeFromChat(Socket clientSocket){

            if(clients.containsKey(clientSocket)) {
                ClientObserver clientObserver = clients.get(clientSocket);
                deleteObserver(clientObserver);
                clients.remove(clientSocket);
                return true;
            }

            else return false;
        }

        public void notifyClients(String message){
            setChanged();
            notifyObservers(message);
        }

    }


    private class ClientHandler implements Runnable {
        private ArrayList<ServerMain.ChatGroup> chatGroups;
        private BufferedReader reader;
        private Socket sock;

        public ClientHandler(Socket clientSocket) throws IOException {
            chatGroups = new ArrayList<>();
            sock = clientSocket;
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        }

        public void run() {
            String message;
            try {
                // padding to decipher message intent
                // chat number second character for everything other than create chat?
                // 0 for real message
                // 1 to create new chat
                // 2 add member to chat
                // 3 to leave chat
                while ((message = reader.readLine()) != null) {
                    System.out.println("read: " + message);

                    // Message structure: INTENT CHATUID sendingIP requestedIP MESSAGE
                    String[] split = message.split(" ");
                    int intent = Integer.parseInt(split[0]);
                    String chatUID = split[1];
                    String ip_address = split[2];
                    String requestedIP = split[3];
                    String temp_msg = message.substring(message.indexOf(split[4]));

                    String returnMsg = "";
                    boolean status;

                    switch (intent) {
                        case realMsg:
                            returnMsg = intent + " " + chatUID + " " + ip_address + " " + requestedIP +  " " + temp_msg;
                            sendMessage(chatUID, returnMsg);
                            break;

                        case createChat:
                            try {
                                int response = createChat(sock, chatUID);
                                if (response != -1) returnMsg = intent + " 1 " + ip_address + " " + requestedIP + " " + chatUID;
                                else returnMsg = intent + " 0 " + ip_address + " " + requestedIP +  " " + chatUID;
                            } catch (IOException e) {
                                returnMsg = intent + " 0 " + ip_address + " " + requestedIP +  " " + chatUID;
                            }
                            sendMessage(chatUID, returnMsg);
                            break;

                        case addToChat:
                            // temp_msg = PORT
                            int port = Integer.parseInt(temp_msg);
                            status = addToChat(port, chatUID);
                            System.out.println(status);
                            if (status) returnMsg = intent + " 1 " + ip_address + " " + chatUID + " " + temp_msg;
                            else returnMsg = intent + " 0 " + ip_address + " " + chatUID + " " + temp_msg;
                            sendMessage(chatUID, returnMsg);
                            break;

                        case leaveChat:
                            status = removeFromChat(sock, chatUID);
                            // send chatUID as temp_msg
                            if (status) returnMsg = intent + " 1 " + ip_address + " " + requestedIP +  " " + chatUID;
                            else returnMsg = intent + " 0 " + ip_address + " " + requestedIP +  " " + chatUID;
                            sendMessage(chatUID, returnMsg);
                            break;

                        case closeSocket:
                            clientSockets.remove(sock);
                            sockets.remove(sock);
                            break;

                        case joinBroadcast:
                            if (!broadcastUID.equals("")) chatUID = broadcastUID;
                            int new_port = Integer.parseInt(temp_msg);
                            status = addToBroadcast(new_port,chatUID);
                            if(status) returnMsg = intent + " 1 " + ip_address + " " + requestedIP + " " + chatUID;
                            else returnMsg = intent + " 0 " + ip_address + " " + requestedIP + " " + chatUID;
                            sendMessage(chatUID,returnMsg);
                            break;

                        default: break;

                    }
                    //setChanged();
                    //notifyObservers(message);
                }
            } catch (SocketException e) {
                System.out.println("Socket disconnected");
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

