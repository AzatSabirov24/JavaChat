package server.chat.handler;

import client.ChatController;
import server.chat.MyServer;
import server.chat.User;
//import server.chat.auth.AuthService;
import server.chat.auth.BaseAuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {
    private final MyServer myServer;
    private final Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;


    public static final String AUTH_CMD_PREFIX = "/auth"; // login + password
    public static final String AUTHOK_CMD_PREFIX = "/authok"; // + username
    public static final String AUTHERR_CMD_PREFIX = "/autherr"; // + error msg
    public static final String CLIENT_MSG_CMD_PREFIX = "/clientMsg"; // + msg
    public static final String SERVER_MSG_CMD_PREFIX = "/serverMsg"; // + msg
    public static final String PRIVATE_MSG_CMD_PREFIX = "/w"; // sender + p + msg
    public static final String END_CMD_PREFIX = "/end"; // end
    private String username;


    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        this.clientSocket = socket;
    }


    public void handle() throws IOException {

        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException | SQLException e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }

    private void authentication() throws IOException, SQLException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthCommand(message);
                if (isSuccessAuth) {
                    break;
                }
            } else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Ошибка авторизации");
            }
        }
    }

    private boolean processAuthCommand(String message) throws IOException, SQLException {

        String[] parts = message.split("\\s+", 3);
        String login = parts[1];
        String password = parts[2];

        BaseAuthService baseAuthService = myServer.getAuthService();

        username = baseAuthService.selectUser(login, password);

        if (username != null) {
            if (myServer.isUsernameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                return false;

            }


            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);

            myServer.broadcastMessage(String.format(">>> %s присоединился к чату", username),this, true);
            myServer.subscribe(this);

            return true;


        } else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
            return false;
        }

    }

    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();
            System.out.println("message | " + username + ": " + message);
            if (message.startsWith(END_CMD_PREFIX)) {
                return;
            } else if (message.startsWith(PRIVATE_MSG_CMD_PREFIX)) {
                String[] parts = message.split("\\s+", 3);
                String recipient = parts[1];
                String privateMessage = parts[2];
                myServer.sendPrivateMessage(this, recipient, privateMessage);
            } else {
                myServer.broadcastMessage(message, this, true);
            }
        }

    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", CLIENT_MSG_CMD_PREFIX, sender, message));
    }
}
