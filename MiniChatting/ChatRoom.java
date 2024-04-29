package com.example.MiniChatting;

import java.util.HashMap;
import java.util.Map;

public class ChatRoom {
    private int id;
    Map<String, ChatThread> clients = new HashMap<>();


    public ChatRoom(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void addClient(ChatThread client) {
        clients.put(client.getName(), client);
    }

    public void removeClient(ChatThread client) {
        clients.remove(client.getName());
    }

    public void broadcastMessage(String sender, String message) {
        for (ChatThread client : clients.values()) {
            client.sendMessage(sender, message);
        }
    }
}

