package me.darkmun.blockcitytycoonstructures;

import org.bukkit.Bukkit;

import java.sql.*;
import java.util.logging.Level;

public class Database {
    private Connection connection = null;
    private int keepAlive = 0;

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Bukkit.getScheduler().cancelTask(keepAlive);
            java.util.Properties conProperties = new java.util.Properties();
            conProperties.put("user", "u95570_LRzuS0M9U7");
            conProperties.put("password", "uMGeUJbmt!oH^FYk^I1VSSTW");
            conProperties.put("autoReconnect", "true");
            conProperties.put("maxReconnects", "15");
            String url = "jdbc:mysql://mysql2.joinserver.xyz:3306/s95570_BlockCityTycoon";
            connection = DriverManager.getConnection(url, conProperties);
            keepAlive = Bukkit.getScheduler().runTaskTimerAsynchronously(BlockCityTycoonStructures.getPlugin(), () -> {
                try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM chunk_data")) {
                    ps.executeQuery();
                } catch (SQLException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Keep alive failed", ex);
                    ex.printStackTrace();
                }
            }, 0, 72000).getTaskId();
        }
        return connection;
    }

    public void initializeDatabase() throws SQLException {
        Statement statement = getConnection().createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS chunk_data(structure VARCHAR(30), upgrade VARCHAR(30), chunk VARCHAR(30), data BLOB, bit_mask INTEGER, ground_up_continuous BOOLEAN, tile_entities BLOB, paintings BLOB, item_frames BLOB)";
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
