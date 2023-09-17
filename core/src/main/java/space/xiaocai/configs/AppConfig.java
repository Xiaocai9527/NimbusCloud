package space.xiaocai.configs;

import space.xiaocai.util.LogUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE_NAME = "config.properties";

    private static class AppConfigHolder {
        private static final AppConfig instance = new AppConfig();
    }

    private final Properties properties;

    public static AppConfig getInstance() {
        return AppConfigHolder.instance;
    }

    private AppConfig() {
        properties = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE_NAME)) {
            properties.load(input);
        } catch (IOException e) {
            LogUtil.logInfo("AppConfig load error:%s", e);
        }
    }

    public String getChatKey() {
        return properties.getProperty("chat_key");
    }

    public void setChatKey(String chatKey) {
        properties.setProperty("chat_key", chatKey);
        updatePropertiesFile();
    }

    private void updatePropertiesFile() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(CONFIG_FILE_NAME)) {
            properties.store(fileOutputStream, null);
        } catch (IOException e) {
            LogUtil.logInfo("updatePropertiesFile error:%s", e);
        }
    }

    public String getChatUrl() {
        return properties.getProperty("chat_url");
    }

    public String getChatUrlStandby() {
        return properties.getProperty("chat_url_standby");
    }

    public String getUpdateTokenUrl() {
        return properties.getProperty("update_token_url");
    }

    public String getAuthorization() {
        return properties.getProperty("authorization");
    }

    public String getJdbcUrl() {
        return properties.getProperty("jdbc_url");
    }

    public String getSqlUserName() {
        return properties.getProperty("sql_user_name");
    }

    public String getSqlPwd() {
        return properties.getProperty("sql_pwd");
    }

    public String getWebName() {
        return properties.getProperty("web_name");
    }

    public String getWebPwd() {
        return properties.getProperty("web_pwd");
    }

}
