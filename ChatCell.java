/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/

package assignment7;

import java.util.Observable;
import java.util.UUID;

public class ChatCell {
    private String name;
    private String serverIP;
    private int chatNum;
    private String chatUID;


    public ChatCell(String name, String serverIP, int chatNum) {
        this.name = name;
        this.serverIP = serverIP;
        this.chatNum = chatNum;
        this.chatUID = UUID.randomUUID().toString();
    }

    public ChatCell(String name, String serverIP, int chatNum, String chatUID) {
        this.name = name;
        this.serverIP = serverIP;
        this.chatNum = chatNum;
        this.chatUID = chatUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getChatNum() { return chatNum; }

    public String getChatUID() { return chatUID; }
}