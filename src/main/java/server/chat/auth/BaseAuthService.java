package server.chat.auth;

import server.chat.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;


public class BaseAuthService  {

    public static Connection connection;
    public static Statement stmt;
    public static ResultSet rs;



    public static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\Азат\\Desktop\\ЧАТ\\JavaFX_Chat\\auth.db");
        stmt = connection.createStatement();
    }

    public static void disconnect() throws SQLException {
        connection.close();
    }

    public static String selectUser(String login, String password) throws SQLException {
        rs = stmt.executeQuery(String.format("SELECT password, username FROM auth WHERE login = '%s'", login));
        if (rs.isClosed()) {
            return null;
        }

        String usernameDB = rs.getString("username");
        String passwordDB = rs.getString("password");

        String username = null;
        if (passwordDB != null) {
            username = (passwordDB.equals(password)) ? usernameDB : null;
        }
        return username;
    }




//    @Override
//    public void startAuthentication() {
//        System.out.println("Старт аутентификации");
//    }
//
//    @Override
//    public void endAuthentication() {
//        System.out.println("Окончание аутентификации");
//    }
}
