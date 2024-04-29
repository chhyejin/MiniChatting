package com.example.MiniChatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatThread extends Thread {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ChatRoom currentRoom;
    private String userName;
    private Set<String> userManage = new HashSet<>(); //사용자 관리
    private static int roomNumber = 1;
    private static Map<Integer, ChatRoom> chatRooms = new HashMap<>(); //채팅방
    private static Map<String, Integer> userInRoom = new HashMap<>(); //방과 사용자 현재 채팅방 목록을
    private static Set<String> disturb= new HashSet<>(); //방해금지 모드에 해당하는 사람

    public ChatThread(Socket socket, Set<String> userManage) {
        this.socket = socket;
        this.userManage = userManage;
    }

    @Override
    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                userName = reader.readLine(); //닉네임을 읽어
                if (!userManage.contains(userName)) { //닉네임 같은게 없다면 닉네임 중복 확인 부분
                    writer.println("VALID");
                    userManage.add(userName);
//                        sendMessageToClient("manage"+Usermanage); //이름 관리 set 확인
                    System.out.println(userName + " 닉네임의 사용자가 연결했습니다.");
                    InetAddress localAddress = InetAddress.getLocalHost();
                    System.out.println(userName + " 닉네임의 클라이언트의 접속 IP 주소: " + localAddress.getHostAddress());

                    sendMessageToClient("성공적으로 서버와 연결되었습니다.");
                    break;
                } else {
                    writer.println("UNVALID"); //닉네임 중복 처리
                }
            }
            String message;
            while ((message = reader.readLine()) != null) {
                handleMessage(message);
            }
        } catch (SocketException e) {
            System.out.println(userName + " 닉네임의 사용자가 연결을 끊었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleMessage(String message) {
        if (message.startsWith("/")) {
            String[] parts = message.split(" ");
            String command = parts[0];
            switch (command) {
                case "/list":
                    showRoomList();
                    break;
                case "/create":
                    createRoom();
                    break;
                case "/join":
                    if (parts.length > 1) {
                        joinRoom(Integer.parseInt(parts[1]));
                    } else {
                        sendMessageToClient("사용법: /join [방 번호]");
                    }
                    break;
                case "/exit":
                    exitRoom();
                    break;
                case "/users":
                    showUserInfo();
                    break;
                case "/roomusers":
                    showRoomUser(userName);
                    break;
                case "/whisper":
                    whisper(message);
                    break;
                case "/nodisturb":
                    doNotDisturb();
                    break;
                case "/dodisturb":
                    doDisturb();
                    break;
                case "/bye":
                    sendMessageToClient("서버와의 연결이 해제 되었습니다.");
                    userManage.remove(userName);
                    writer.println("/bye");
                    break;
                default:
                    sendMessageToClient("잘못 입력하셨습니다.");
            }
        } else {
            if (currentRoom != null) {
                currentRoom.broadcastMessage(userName, message);
            } else {
                sendMessageToClient("아직 채팅방에 들어가지 않았습니다.");
            }
        }
    }

    private void sendMessageToClient(String message) { //그냥 노멀한 메시지
        writer.println(message);
    }

    void sendMessage(String sender, String message) { //사용자 이름과 함께 메시지
        if (disturb.contains(userName)) {
            return;
        }
        writer.println(sender+" : "+message);
    }

    private void showRoomList() { //채팅방 리스트 출력
        if (chatRooms.isEmpty()) {
            sendMessageToClient("현재 생성된 채팅방이 없습니다.");
        } else {
            StringBuilder sb = new StringBuilder("생성된 방\n");
            for (ChatRoom room : chatRooms.values()) {
                sb.append(room.getId()).append("번방\t");
            }
            sendMessageToClient(sb.toString());
//                sendMessageToClient("생성된 방");
//                for(ChatRoom room: chatRooms.values()){
//                    sendMessageToClient(room.getId()+" 번방\t");
//                }
        }
    }

    private void showUserInfo() { //현재 접속 중인 모든 사람들
        sendMessageToClient("현재 접속 중인 모든 사람들");
        for (String s : userManage) {
            sendMessageToClient(s);
        }
    }

    public void showRoomUser(String userName) { //해당 채팅방에 있는 사람들 출력

        int foundValue = 0;
        for (Map.Entry<String, Integer> integerStringEntry : userInRoom.entrySet()) {
            System.out.println(integerStringEntry);
            if (integerStringEntry.getKey().equals(userName)) {
                foundValue = integerStringEntry.getValue();
                System.out.println(foundValue); //서버에 이름 : 채팅방
                break;
            }
        }
        for (Map.Entry<String, Integer> entry : userInRoom.entrySet()) {
            if (entry.getValue() == foundValue) {
                sendMessageToClient("현재 채팅방의 접속자 " + entry.getKey());
            }else{
                sendMessageToClient("현재 접속 중인 채팅방이 없습니다.");
            }
        }
    }

    private void whisper(String message) { //귓속말
        String[] parts = message.split(" ", 3);
        if (parts.length != 3) {
            sendMessageToClient("사용법: /whisper [보낼 대상 닉네임] [메시지]");
            return;
        }

        String targetUsername = parts[1];
        String whisperMessage = parts[2];

        ChatThread targetClient = findClientByUsername(targetUsername);//해당 대상 클라이언트에 메시지 전송
        if (targetClient != null) {
            targetClient.sendMessageToClient("[귓속말 전송] " + userName + ": " + whisperMessage);
            sendMessageToClient("귓속말을 전송했습니다.");
        } else {
            sendMessageToClient("귓속말을 보낼 대상이 없습니다.");
        }
    }

    private ChatThread findClientByUsername(String username) {// 사용자 이름에 해당하는 클라이언트 찾기
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t instanceof ChatThread) {
                ChatThread client = (ChatThread) t;
                if (client.userName.equals(username)) {
                    return client;
                }
            }
        }
        return null;
    }

    private void createRoom() { //채팅방 생성
        int roomId = roomNumber++;
        ChatRoom room = new ChatRoom(roomId);
        chatRooms.put(roomId, room);
        joinRoom(roomId);
        sendMessageToClient("방 번호 " + roomId + " 가 생성되었습니다.");
    }

    private void joinRoom(int roomId) { //채팅방 입장
        if (chatRooms.containsKey(roomId)) {
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }
            currentRoom = chatRooms.get(roomId);
            currentRoom.addClient(this);
            sendMessageToClient(userName + " 닉네임이 " + roomId + " 번방에 입장했습니다.");
            userInRoom.put(userName, roomId); //key 방 번호, value userName
            System.out.println(userName + " 닉네임이 " + roomId + " 번방에 입장했습니다.");
        } else {
            sendMessageToClient(roomId + " 번방은 존재하지 않습니다.");
        }
    }

    private void doNotDisturb() { //방해금지 모드
        sendMessageToClient("방해금지 모드를 활성화 했습니다.");
        sendMessageToClient(userName + " 님께서 방해금지 모드를 활성화 했습니다.");
        disturb.add(userName);
    }

    private void doDisturb() { //방해금지 해제
        sendMessageToClient("방해금지 모드를 해제했습니다.");
        sendMessageToClient(userName + " 님께서 방해금지 모드를 해제 했습니다.");
        disturb.remove(userName);
    }

    private void exitRoom() { //채팅방 나가기
        if (currentRoom != null) {
            currentRoom.removeClient(this);
            sendMessageToClient(userName + "님이 이 방을 나갔습니다.");
            userInRoom.remove(userName); //사용자 제거 현재 채팅방 목록에서
            if (currentRoom.clients.isEmpty()) {
                chatRooms.remove(currentRoom.getId()); //방 번호 제거
                //                    userInRoom.remove(currentRoom.getId()); //유저
                sendMessageToClient("방 번호 " + currentRoom.getId() + "가 제거되었습니다.");
                userInRoom.remove(currentRoom.getId()); //같은 방 번호 정보 삭제
                System.out.println("방 번호 " + currentRoom.getId() + "가 제거되었습니다.");
            } else {
                sendMessageToClient("클라이언트가 방에 존재합니다. 그러므로 방을 제거할 수 없습니다.");
            }
            currentRoom = null;
        } else {
            sendMessageToClient("들어간 방이 없었습니다.");
        }
    }

}
