/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/


package assignment7;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import sun.rmi.runtime.Log;

import java.util.UUID;


public class ClientMain extends Application {
	private BufferedReader reader;
	private PrintWriter writer;
	protected Socket sock;
	private Thread readerThread;
    protected String clientIP;
    protected boolean isClosed = false;
    protected List<Chat> activeChats;
    protected String clientPort;
    protected String selectedChatIP;

    private ChatViewController viewController = new ChatViewController(this);

    // message padding to decipher message intent
    private final int realMsg = 0;
    private final int createChat = 1;
    private final int addToChat = 2;
    private final int leaveChat = 3;
    private final int joinBroadcastGroup = 5;

    protected enum MsgIntent {
    	realMSG(0),
		createChat(1),
		addToChat(2),
		leaveChat(3),
        shutdown(4),
        joinBroadcastGroup(5);

    	private final int value;
    	MsgIntent (int value) {
    		this.value = value;
		}

    	public String getValue() {
    		return String.valueOf(value);
		}
	}


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        int numChats = 2;
        for (int i = 0; i < numChats; i++) {
            if (i == 0) this.initView(primaryStage);
            else {
                ClientMain client = new ClientMain();
                Stage newStage = new Stage();
                client.initView(newStage);
            }
        }

    }

	protected void initView(Stage primaryStage) {
		try {
            ChatViewController.LoginScreen loginScreen = viewController.loginScreen;
            loginScreen.start(primaryStage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setUpNetworking(String ip) throws Exception {
        activeChats = new ArrayList<>();
		//@SuppressWarnings("resource")
        try {
            sock = new Socket(ip, 4242);
            clientPort = "" + sock.getLocalPort();
            System.out.println("New Port: " + clientPort);
            //InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new PrintWriter(sock.getOutputStream());
            readerThread = new Thread(new IncomingReader());
            readerThread.start();
        } catch (SocketException e){
            System.out.println("Invalid ip");
        }
//		System.out.println("networking established");
//		send("A new challenger has appeared");
	}

	protected void send(String intent, String chatUID, String ip_address, String requestedIP, String message) {
        // 0 for real message
        // 1 to create new chat
        // 2 to add new member to chat
        // 3 to leave chat
        try {
            writer.println(intent + " " + chatUID + " " + ip_address + " " + requestedIP + " " + message);
            writer.flush();
            viewController.outgoing.clear();
            viewController.outgoing.requestFocus();
        } catch (NullPointerException e) {
            System.out.println("Invalid Port Address");
        }
	}

	protected String generateUID() {
        return UUID.randomUUID().toString();
    }

	private void addChat(ChatCell chatCell, String ip_address, String uniqueIdentifier) {
		viewController.chatHistory.getItems().add(chatCell);
		viewController.chatHistory.getSelectionModel().select(viewController.chatHistory.getItems().size()-1);
//		viewController.selectedChatCell = chatCell;

		Chat newChat = new Chat(chatCell.getName(),ip_address, clientPort);
		newChat.chatUID = uniqueIdentifier;
		activeChats.add(newChat);
	}

	// remove chat from GUI
	private void removeFromChat(int chatNum){
		viewController.chatHistory.getItems().remove(activeChats.get(chatNum));
		activeChats.remove(chatNum);

    }

    private void removeFromChat(String chatUID) {
        for (Chat chat: activeChats) {
            if (chat.chatUID.equals(chatUID)) {
                viewController.chatHistory.getItems().remove(chat);
                activeChats.remove(chat);
                break;
            }
        }
    }

	class IncomingReader implements Runnable {
		public void run() {
			String message;
			try {
                // 0 for real message
                // 1 to tell client their chat has been created, chat number after
                // 2 to tell client another client has joined chat
                // 3 to tell client they've been removed from the chat chatNum
			    while(!isClosed) {
                    while ((message = reader.readLine()) != null) {


                        // Message structure: INTENT STATUS sendingIP requestedIP MESSAGE
                        String[] split = message.split(" ");
                        int intent = Integer.parseInt(split[0]);
                        String ip_status = split[1];
                        String ip_address = split[2];
                        String requestedIP = split[3];
                        String temp_msg = message.substring(message.indexOf(split[4]));

                        switch (intent) {
                            case realMsg:

                                String receivingChatUID = ip_status;
                                Chat updatedChat = null;

                                // Receiving a message in the non-primary chat
                                for (Chat chat : activeChats) {
                                    if (chat.chatUID.equals(receivingChatUID)) {
                                        if (!chat.getOutputLog().equals("")) {
                                            chat.setOutputLog(chat.getOutputLog() + "\n" + requestedIP + ": " + temp_msg);
                                        } else {
                                            chat.setOutputLog(requestedIP + ": " + temp_msg);
                                        }
                                        updatedChat = chat;
                                    }
                                }

                                if (viewController.selectedChatCell != null) {
                                    if (viewController.selectedChatCell.getChatUID().equals(receivingChatUID)) {
                                        if (updatedChat != null) {
                                            viewController.incoming.setText(updatedChat.getOutputLog());
                                        }
                                    }
                                }

                                break;

                            case createChat:
                                // here, temp_msg = chatUID
                                if (ip_status.equals("1")) {
                                    // DONE: create new chat tab for client window, add chatNum to ChatCell
                                    ChatCell chatCell = new ChatCell("Chat " + activeChats.size(), ip_address, activeChats.size(), temp_msg);
                                    addChat(chatCell, ip_address, chatCell.getChatUID());
                                }
                                break;

                            case addToChat:
                                // check if ip address is the one requested to add
                                // if it is, add a new chat window to client
                                // if not, update list of who is in current chat
                                if(ip_status.equals("1")) {
                                    // here, temp_msg = PORT
                                    int port = Integer.parseInt(temp_msg);
                                    if (sock.getLocalPort() == port) {
                                        // TODO: add new chat tab to client window
                                        ChatCell chatCell = new ChatCell("Chat " + activeChats.size(), ip_address, activeChats.size(), requestedIP);
                                        addChat(chatCell, ip_address, requestedIP);
                                    } else {
                                        // TODO: add client to chat member list
										System.out.println("That user cannot be found");
                                        // not sure if this is necessary
                                    }
                                }
                                break;

                            case leaveChat:
                                // check if ip address is current client's or not
                                // if it is, remove chat from client window
                                // if not, update list of who is in current chat

                                // temp_msg is chatUID

                                if(ip_status.equals("1")) {
                                    if (clientIP.equals(ip_address)) {
                                        removeFromChat(temp_msg);
                                    }
                                }
                                break;

                            case joinBroadcastGroup:
                                // Message structure: INTENT STATUS sendingIP requestedIP MESSAGE
                                if (activeChats.size() == 0) {
                                    if (ip_status.equals("1")) {
                                        ChatCell chatCell = new ChatCell("Broadcast", ip_address, activeChats.size(), temp_msg);
                                        addChat(chatCell, ip_address, chatCell.getChatUID());
                                    }
                                }
                                break;

                            // updateName
                            //clientName = requestedIP;

                            default: break;
                        }
                    }
                }
			} catch (SocketException e) {
			    System.out.println(clientIP + " ended");
            } catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
