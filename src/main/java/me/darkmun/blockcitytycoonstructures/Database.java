package me.darkmun.blockcitytycoonstructures;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private Connection connection = null;

    public Connection getConnection() throws SQLException {
        if (connection != null) {
            return connection;
        }

        java.util.Properties conProperties = new java.util.Properties();
        conProperties.put("user", "u95570_LRzuS0M9U7");
        conProperties.put("password", "uMGeUJbmt!oH^FYk^I1VSSTW");
        conProperties.put("autoReconnect", "true");
        conProperties.put("maxReconnects", "5");
        String url = "jdbc:mysql://mysql2.joinserver.xyz:3306/s95570_BlockCityTycoon";
        connection = DriverManager.getConnection(url, conProperties);
        return connection;
    }

    public void initializeDatabase() throws SQLException {
        Statement statement = getConnection().createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS chunk_data(structure VARCHAR(30), upgrade VARCHAR(30), chunk VARCHAR(30), data BLOB, bit_mask INTEGER, ground_up_continuous BOOLEAN, tile_entities BLOB)";
        statement.execute(sql);
        statement.close();
    }

    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
