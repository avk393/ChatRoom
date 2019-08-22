/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/

package assignment7;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Observable;
import java.util.Observer;

public class ClientObserver extends PrintWriter implements Observer {

    public ClientObserver(OutputStream stream){
        super(stream);
    }

    @Override
    public void update(Observable o, Object arg) {
        // 0 for real message
        // 1 to tell client their chat has been created, chat number after; 11 success 10 failure
        // 2 to tell client some other client has been added to chat, along with ip address of added member
        // 3 to tell client they've been removed from the chat num, updates rest of clients in that chat with ip_address of exiting client
        println(arg);
        flush();
    }

    public void send(String message){
        println(message);
        flush();
    }
}
