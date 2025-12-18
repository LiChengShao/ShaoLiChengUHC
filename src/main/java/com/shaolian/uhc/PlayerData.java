package com.shaolian.uhc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.event.Listener;

public class PlayerData implements Listener {

    private Main plugin;
    private Connection connection;

    public PlayerData(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        connectDatabase();
        createTable();
        plugin.getLogger().info("PlayerData初始化完成");
    }

    private void connectDatabase() {
        try {
            // 获取插件的data文件夹
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // 在插件data文件夹中创建数据库文件
            File dbFile = new File(dataFolder, "uhc.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            // 连接数据库
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("成功连接数据库");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("连接数据库失败: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_stats ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "player_name TEXT NOT NULL UNIQUE,"  // 添加 UNIQUE 约束
                + "kills INTEGER NOT NULL,"
                + "deaths INTEGER NOT NULL"
                + ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("成功创建数据表");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("创建数据表失败: " + e.getMessage());
        }
    }

    public void updatePlayerStats(String playerName, int kills, int deaths) {
        String sql = "INSERT INTO player_stats(player_name, kills, deaths) VALUES(?, ?, ?) "
                + "ON CONFLICT(player_name) DO UPDATE SET "
                + "kills = kills + excluded.kills, "
                + "deaths = deaths + excluded.deaths;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, kills);
            pstmt.setInt(3, deaths);
            pstmt.executeUpdate();
            plugin.getLogger().info("成功更新玩家 " + playerName + " 的统计数据");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("更新玩家统计数据失败: " + e.getMessage());
        }
    }
}