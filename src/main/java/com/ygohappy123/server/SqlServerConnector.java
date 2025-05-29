package com.ygohappy123.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqlServerConnector {
    Connection con;
    Statement stmt;
    String url;
    String driver;

    public SqlServerConnector(String url, String driver) {
        this.url = url;
        this.driver = driver;
    }

    public Statement getConnection() {
        try {
            Class.forName(driver);
            con = DriverManager.getConnection(url);
            stmt = con.createStatement();
            return stmt;
        } catch (Exception ex) {
            System.out.println("Failed to connect to database.");
            return null;
        }
    }

    public int executeUpdate(String sql) {
        try {
            stmt = getConnection();
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            return -1;
        }
    }

    public ResultSet loadData(String sql) {
        try {
            stmt = getConnection();
            return stmt.executeQuery(sql);
        } catch (Exception e) {
            return null;
        }
    }

    public String executeQuery(String sql) {
        try {
            stmt = getConnection();
            ResultSet rs = stmt.executeQuery(sql);
            StringBuilder seatList = new StringBuilder();

            while (rs.next()) {
                String id = String.valueOf(rs.getInt(1));
                String sold = String.valueOf(rs.getInt(2));
                String block = String.valueOf(rs.getInt(3));
                String item = id + " " + sold + " " + block;
                seatList.insert(0, item + " ");
            }
            return seatList.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
