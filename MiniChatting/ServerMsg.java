package com.example.MiniChatting;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerMsg implements Runnable {
    private BufferedReader in;

    public ServerMsg(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String serverLine;
            while ((serverLine = in.readLine()) != null) {
                System.out.println(serverLine); // 서버로부터 받은 메시지를 출력
            }
        } catch (IOException e) {
            System.out.println("종료!");
        }
    }
}
