/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/

package assignment7;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.SocketException;
import java.util.ArrayList;

public class ChatViewController extends Application {

    protected TextArea incoming = new TextArea();
    protected TextField outgoing = new TextField();
    protected TextField joinChatWithIPField = new TextField();
    protected TableView chatHistory = new TableView();
    protected LoginScreen loginScreen = new LoginScreen();
    private Stage window;
    private HBox view;

    protected ChatCell selectedChatCell;
    private ClientMain client;

    public ChatViewController(ClientMain client) {
        this.client = client;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initView(primaryStage);
    }

    private void initView(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Your Chat Port: " + client.clientPort);
        window.setMinWidth(675);
        window.setMinHeight(400);
        window.setMaxWidth(675);
        window.setMaxHeight(400);

        /* === Chat Window HBox === */
        view = new HBox();
        view.setSpacing(10);
        Scene messagesUI = new Scene(view, window.getMaxWidth(),window.getMaxHeight());

        VBox chatHistoryPane = new VBox();
        chatHistoryPane.setMaxWidth(125);

        /* === TableView for Existing Chats === */
        TableColumn<String, ChatCell> openChatsColumn = new TableColumn<>("Chat history");
        openChatsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        openChatsColumn.setMinWidth(chatHistoryPane.getMaxWidth());
        chatHistory.getColumns().add(openChatsColumn);
        chatHistory.addEventHandler(MouseEvent.MOUSE_CLICKED, new SelectionEventHandler());
        chatHistory.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            System.out.println("obs: " + observable + "\nold: " + oldValue + "\nnew: " + newValue);

            selectedChatCell = (ChatCell) newValue;
            client.selectedChatIP = selectedChatCell.getServerIP();

            for (Chat chat : client.activeChats) {
                if (chat.chatUID.equals(selectedChatCell.getChatUID())) {
                    window.setTitle("Chat: " + chat.getName() + " | Port: " + client.clientPort);
                    incoming.setText(chat.outputLog);
                    outgoing.setText(chat.inputMsg);
                }
            }
            System.out.println(client.clientPort + " > " + selectedChatCell.getChatUID());
        }));

        Button addChatBtn = new Button("Create New Chat");
        addChatBtn.setOnAction(event -> {
            // create a chat
            try {
                client.send(ClientMain.MsgIntent.createChat.getValue(), client.generateUID(), client.clientIP, client.clientIP, "-1");
            } catch (NullPointerException e) {
                System.out.println("Server not initiated");
            }
        });
        addChatBtn.setMinWidth(chatHistoryPane.getMaxWidth());

        /* === Buttons === */
        Button close = new Button("Close Client");
        /* Done: implement a socket.close() functionality to avoid exception when clicking close button, also need to tell server to remove client from all chats socket is currently in */
        // we can make a for loop and leave every chat current client is in
        close.setOnAction(event -> {
            try{
                // Remove self from all active chats
                for (int i = client.activeChats.size()-1; i >= 0; i--) {
                    client.send(ClientMain.MsgIntent.leaveChat.getValue(), client.activeChats.get(i).chatUID, client.clientIP, client.clientIP, "shutdown");
                }
                client.send(ClientMain.MsgIntent.shutdown.getValue(), "-1", client.clientIP, client.clientIP, "shutdown");

                client.isClosed = true;
                client.sock.close();
                window.close();

            }catch (SocketException e){
                System.out.println("Found socket exception");
            }catch (IOException e) {
                System.out.println("IOE");
            } catch (NullPointerException e) {
                window.close();
            }
        });

        Button restartBtn = new Button("Restart Client");
        restartBtn.setOnAction(event -> {
            try{
                // Remove self from all active chats
                for (int i = client.activeChats.size()-1; i >= 0; i--) {
                    client.send(ClientMain.MsgIntent.leaveChat.getValue(), client.activeChats.get(i).chatUID, client.clientIP, client.clientIP, "shutdown");
                }
                client.send(ClientMain.MsgIntent.shutdown.getValue(), "-1", client.clientIP, client.clientIP, "shutdown");

                client.isClosed = true;
                client.sock.close();
                window.close();
                loginScreen.start(primaryStage);

            } catch (SocketException e){
                System.out.println("Found socket exception");
            } catch (IOException e) {
                System.out.println("IOE");
            } catch (NullPointerException e) {
                System.out.println("Server not initialized");
            } catch (Exception e) {
                System.out.println("Error going to login screen");
            } finally {
                window.close();
            }
            try {
                loginScreen.start(primaryStage);
            } catch (Exception e) {}
        });

        Button addUserToChat = new Button("Add User");
        addUserToChat.setOnAction(event -> {
            String newChatUID = client.generateUID();
            // add the desired user to the new chat
            if (chatHistory.getSelectionModel().getSelectedItem() != null) {
                ChatCell selectedCell = (ChatCell) chatHistory.getSelectionModel().getSelectedItem();
                client.send(ClientMain.MsgIntent.addToChat.getValue(), selectedCell.getChatUID(), client.clientIP, selectedCell.getChatUID(), joinChatWithIPField.getText());
            } else {
                // create a chat
                client.send(ClientMain.MsgIntent.createChat.getValue(), newChatUID, client.clientIP, client.clientIP, "-1");
                // Add user to new chat
                client.send(ClientMain.MsgIntent.addToChat.getValue(), newChatUID, client.clientIP, client.clientIP, joinChatWithIPField.getText());
            }
            joinChatWithIPField.setText("");
        });
        addUserToChat.setMinWidth(100);

        Button removeChat = new Button("Leave Chat");
        removeChat.setOnAction(event -> {
            // Done: need to append chat # with a space in front of ip address
            ChatCell selectedChat = (ChatCell) chatHistory.getSelectionModel().getSelectedItem();
            if (!selectedChat.getName().equals("Broadcast")) {
                chatHistory.getItems().remove(selectedChat);
                client.activeChats.remove(selectedChat);
            }
            try {
                client.send(ClientMain.MsgIntent.leaveChat.getValue(), selectedChatCell.getChatUID(), client.selectedChatIP, client.clientIP, "goodbye");
                incoming.setText("");
            } catch (NullPointerException e) { }
        });

        Button sendMsgBtn = new Button("Send Message");
        sendMsgBtn.setOnAction((event -> {
            try {
                client.send(ClientMain.MsgIntent.realMSG.getValue(), selectedChatCell.getChatUID(), client.selectedChatIP, client.clientIP, outgoing.getText());
            } catch (NullPointerException e) {}
        }));

        /* === Text Boxes === */
        // DONE: make text box client can enter IP address into to add new client to chat?
        joinChatWithIPField = new TextField();
        joinChatWithIPField.setOnAction(event -> {
            String newChatUID = client.generateUID();
            // add the desired user to the new chat
            if (chatHistory.getSelectionModel().getSelectedItem() != null) {
                ChatCell selectedCell = (ChatCell) chatHistory.getSelectionModel().getSelectedItem();
                client.send(ClientMain.MsgIntent.addToChat.getValue(), selectedCell.getChatUID(), client.clientIP, selectedCell.getChatUID(), joinChatWithIPField.getText());
            } else {
                // create a chat
                client.send(ClientMain.MsgIntent.createChat.getValue(), newChatUID, client.clientIP, client.clientIP, "-1");
                // Add user to new chat
                client.send(ClientMain.MsgIntent.addToChat.getValue(), newChatUID, client.clientIP, client.clientIP, joinChatWithIPField.getText());
            }
            joinChatWithIPField.setText("");
        });

        incoming.setEditable(false);
        incoming.setMaxWidth(400);

        outgoing.setOnAction(event -> {
            try {
                client.send(ClientMain.MsgIntent.realMSG.getValue(), selectedChatCell.getChatUID(), client.selectedChatIP, client.clientIP, outgoing.getText());
            } catch (NullPointerException e) {}
        });

        /* === Messaging area === */
        GridPane messagesPane = new GridPane();
        messagesPane.setPadding(new Insets(10,10,10,10));
        messagesPane.setVgap(8);
        messagesPane.setHgap(8);
        messagesPane.add(joinChatWithIPField,0,0);
        messagesPane.add(addUserToChat,1, 0);
        messagesPane.add(incoming,0,1);
        messagesPane.add(outgoing,0,4);
        messagesPane.add(sendMsgBtn,1,4);
        messagesPane.add(removeChat,0,6);
        messagesPane.add(close,0,10);
        messagesPane.add(restartBtn,1,10);
        messagesPane.setMaxWidth(550);

        view.getChildren().add(chatHistoryPane);
        view.getChildren().add(messagesPane);


        chatHistoryPane.getChildren().add(addChatBtn);
        chatHistoryPane.getChildren().add(chatHistory);

        window.setWidth(window.getMinWidth());
        window.setHeight(window.getMinHeight());
        window.setAlwaysOnTop(true);
        window.setScene(messagesUI);
        window.show();

        client.send(ClientMain.MsgIntent.joinBroadcastGroup.getValue(),client.generateUID(),client.clientIP, client.clientIP, client.clientPort);
    }

    class SelectionEventHandler implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent t) {
            TableView c = (TableView) t.getSource();
            int index = c.getSelectionModel().getSelectedIndex();

            // User can right click or double click to deselect
            if (t.isSecondaryButtonDown() || t.getClickCount() > 1) {
                client.selectedChatIP = "";
                window.setTitle("Your Chat Port: " + client.clientPort);
                chatHistory.getSelectionModel().clearSelection();

            } else {
                try {
                    selectedChatCell = (ChatCell) chatHistory.getItems().get(index);
                    client.selectedChatIP = selectedChatCell.getServerIP();

                    for (Chat chat : client.activeChats) {
                        if (chat.chatUID.equals(selectedChatCell.getChatUID())) {
                            window.setTitle("Chat: " + chat.getName() + " | Port: " + client.clientPort);
                            incoming.setText(chat.outputLog);
                            outgoing.setText(chat.inputMsg);
                        }
                    }
                    System.out.println(client.clientPort + " > " + selectedChatCell.getChatUID());
                } catch (ArrayIndexOutOfBoundsException e) {
                    client.selectedChatIP = "";
                    window.setTitle("Your Chat Port: " + client.clientPort);
                    chatHistory.getSelectionModel().clearSelection();
                }
            }
        }
    }

    public class LoginScreen extends  Application {
        String desiredIP_Address;
        String desiredServer_Address;
        public LoginScreen() {
            desiredIP_Address = "";
            desiredServer_Address = "";
        }

        @Override
        public void start(Stage primaryStage) throws Exception {

            window = primaryStage;
            window.setTitle("Connect to a Server");
            window.setMinWidth(250);
            window.setMaxWidth(400);
            window.setMinHeight(200);
            window.setMaxHeight(300);

            Label desiredIPLabel = new Label("Your IP: ");
            TextField desiredIPField = new TextField();
            desiredIPField.setText("127.0.0.1");
            desiredIPField.setOnAction(event -> {
                desiredIP_Address = desiredIPField.getText();
            });
            Label desiredServerLabel = new Label("Server: ");
            TextField desiredServerField = new TextField();
            desiredServerField.setText("127.0.0.1");
            desiredServerField.setOnAction(event -> {
                desiredServer_Address = desiredServerField.getText();
            });

            Button joinServerBtn = new Button("Join Server");
            joinServerBtn.setOnAction(event -> {
                try {
//                    window.hide();
                    // if either field is empty, no go
                    if (desiredIPField.getText().equals("") || desiredServerField.getText().equals("")) {
                        System.out.println("You suck");
                    } else {
                        client.setUpNetworking(desiredServerField.getText());
                        client.clientIP = desiredIPField.getText();
                        ChatViewController.this.start(primaryStage);
                    }

                } catch (Exception e) {
                    System.out.println("This is awkward...");
                }
            });
            joinServerBtn.minWidth(window.getWidth());

            Button closeWindowBtn = new Button("Close");
            closeWindowBtn.setOnAction(event -> {
                window.close();
            });
            closeWindowBtn.minWidth(window.getWidth());

            GridPane simpleLoginPane = new GridPane();
            simpleLoginPane.setPadding(new Insets(10,10,10,10));
            simpleLoginPane.setVgap(10);
            simpleLoginPane.setHgap(10);
            simpleLoginPane.add(desiredIPLabel,0,0);
            simpleLoginPane.add(desiredIPField,1,0);
            simpleLoginPane.add(desiredServerLabel,0,1);
            simpleLoginPane.add(desiredServerField,1,1);
            simpleLoginPane.add(joinServerBtn,1,3);
            simpleLoginPane.add(closeWindowBtn,0,5);

            Scene loginUI = new Scene(simpleLoginPane, window.getMaxWidth(), window.getMaxHeight());

            window.setWidth(window.getMinWidth());
            window.setHeight(window.getMinHeight());
            window.setAlwaysOnTop(true);
            window.setScene(loginUI);
            window.show();

        }
    }
}

