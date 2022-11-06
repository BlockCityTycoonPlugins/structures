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

        String url = "jdbc:mysql://mysql2.joinserver.xyz:3306/s95570_BlockCityTycoon";
        connection = DriverManager.getConnection(url, "u95570_LRzuS0M9U7", "uMGeUJbmt!oH^FYk^I1VSSTW");
        return connection;
    }

    public void initializeDatabase() throws SQLException {
        Statement statement = getConnection().createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS chunk_data(structure VARCHAR(30), upgrade VARCHAR(30), chunk VARCHAR(30), data BLOB, bit_mask INTEGER, ground_up_continuous BOOLEAN, tile_entities BLOB)";
        statement.execute(sql);
        statement.close();
    }

}
