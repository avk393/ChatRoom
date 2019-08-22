/* CHAT ROOM <MyClass.java>*
EE422C Project 7 submission by*
<Anith Kandikondae>*  <avk393>*  <16190>*
<Donovan McCray>*  <dom342>*  <16190>*
Slip days used: <1>*
Spring 2019*/

package assignment7;

class Chat {
    String outputLog;
    String inputMsg;
    String name;
    String ip_address;
    String chatUID;
    String port;

    public Chat(String chatName, String ip_address, String port) {
        this.outputLog = "";
        this.inputMsg  = "";
        this.name = chatName;
        this.ip_address = ip_address;
        this.port = port;
    }

    public String getOutputLog() {
        return outputLog;
    }

    public void setOutputLog(String outputLog) {
        this.outputLog = outputLog;
    }

    public void setIP_Address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getInputMsg() {
        return inputMsg;
    }

    public void setInputMsg(String inputMsg) {
        this.inputMsg = inputMsg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}