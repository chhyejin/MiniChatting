package com.example.MiniChatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 12345;
    private static int roomNumber = 1;
    private static Map<Integer, ChatRoom> chatRooms = new HashMap<>(); //채팅방
    private static Map<String, Integer> userInRoom = new HashMap<>(); //방과 사용자

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Set<String> manage = new HashSet<>();
            while (true) {
                Socket socket = serverSocket.accept();
                new ChatThread(socket, manage).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
