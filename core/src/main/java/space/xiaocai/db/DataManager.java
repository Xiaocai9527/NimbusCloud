package space.xiaocai.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import space.xiaocai.configs.AppConfig;
import space.xiaocai.data.ChatResponse;
import space.xiaocai.util.LogUtil;

public class DataManager {
    private static final String DB_NAME = "nimbus_cloud";
    private final DataSource dataSource;

    public DataManager() {
        HikariConfig config = new HikariConfig();
        AppConfig appConfig = AppConfig.getInstance();
        config.setJdbcUrl(appConfig.getJdbcUrl());
        config.setUsername(appConfig.getSqlUserName());
        config.setPassword(appConfig.getSqlPwd());
        config.addDataSourceProperty("connectionTimeout", "1000");
        config.addDataSourceProperty("idleTimeout", "60000");
        config.addDataSourceProperty("maximumPoolSize", "10");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String checkDBQuery = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DB_NAME + "'";
            ResultSet resultSet = statement.executeQuery(checkDBQuery);
            boolean dbExists = resultSet.next();
            resultSet.close();

            if (!dbExists) {
                String createDB = "CREATE DATABASE " + DB_NAME;
                statement.executeUpdate(createDB);
            }

            String useDBQuery = "USE " + DB_NAME;
            statement.executeUpdate(useDBQuery);

            // SQL语句
            String sql = "CREATE TABLE IF NOT EXISTS chat_ai (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title TEXT," +
                    "content TEXT," +
                    "date DATE" +
                    ")";
            statement.executeUpdate(sql);

            LogUtil.logInfo("SQL create success");
        } catch (SQLException e) {
            LogUtil.logInfo("SQL connect error: %s", e);
        }
    }

    // 返回自增主键id
    public long saveChat(ChatResponse chatResponse) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement prepareStatement = connection.prepareStatement(
                    "USE " + DB_NAME)) {
                prepareStatement.execute(); // 选择数据库
            }
            try (PreparedStatement prepareStatement = connection.prepareStatement(
                    "INSERT INTO chat_ai (title, content, date, ipAddress) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                prepareStatement.setString(1, chatResponse.title());
                prepareStatement.setString(2, chatResponse.content());
                Date date = new Date(chatResponse.dateTime());
                prepareStatement.setDate(3, date);
                prepareStatement.setString(4, chatResponse.ipAddress());
                int n = prepareStatement.executeUpdate();
                LogUtil.logInfo("save chat num:%s", n);
                try (ResultSet generatedKeys = prepareStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        LogUtil.logInfo("id:%s", id);
                        return id;
                    }
                }
            }

        } catch (SQLException e) {
            LogUtil.logInfo("saveChat error:%s", e);
        }
        return -1;
    }

    public List<ChatResponse> getChatResponseToday() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement prepareStatement = connection.prepareStatement(
                    "USE " + DB_NAME)) {
                prepareStatement.execute(); // 选择数据库
            }
            try (PreparedStatement prepareStatement = connection.prepareStatement(
                    "SELECT * FROM chat_ai WHERE date=?")) {
                Date date = new Date(System.currentTimeMillis());
                prepareStatement.setDate(1, date);
                List<ChatResponse> chatResponses = new ArrayList<>();
                try (ResultSet resultSet = prepareStatement.executeQuery()) {
                    while (resultSet.next()) {
                        ChatResponse chatResponse = ChatResponse.create(resultSet.getString("content"), resultSet.getString("title"),
                                resultSet.getDate("date").getTime(), 0, resultSet.getString("ipAddress"));
                        chatResponses.add(chatResponse);
                    }
                    return chatResponses;
                }
            }
        } catch (SQLException e) {
            LogUtil.logInfo("getChatResponseToday error:%s", e);
        }
        return Collections.emptyList();
    }

    public List<ChatResponse> getAllChatResponse() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement prepareStatement = connection.prepareStatement(
                    "USE " + DB_NAME)) {
                prepareStatement.execute(); // 选择数据库
            }
            try (PreparedStatement prepareStatement = connection.prepareStatement("SELECT * FROM chat_ai")) {
                List<ChatResponse> chatResponses = new ArrayList<>();
                try (ResultSet resultSet = prepareStatement.executeQuery()) {
                    while (resultSet.next()) {
                        ChatResponse chatResponse = ChatResponse.create(resultSet.getString("content"), resultSet.getString("title"),
                                resultSet.getDate("date").getTime(), 0, resultSet.getString("ipAddress"));
                        chatResponses.add(chatResponse);
                    }
                    return chatResponses;
                }
            }
        } catch (SQLException e) {
            LogUtil.logInfo("getChatResponseToday error:%s", e);
        }
        return Collections.emptyList();
    }

}
