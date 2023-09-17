package space.xiaocai.impl;

import com.google.gson.Gson;
import okhttp3.*;
import space.xiaocai.api.ChatApi;
import space.xiaocai.configs.AppConfig;
import space.xiaocai.data.*;
import space.xiaocai.db.DataManager;
import space.xiaocai.util.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatApiImpl implements ChatApi {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final OkHttpClient client;
    private final Gson gson;
    private final DataManager dataManager;
    private final Map<String, List<ChatRequestMessage>> ipChatMsgRecords;

    public ChatApiImpl(Gson gson, DataManager dataManager) {
        this.gson = gson;
        this.dataManager = dataManager;
        client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .build();
        ipChatMsgRecords = new HashMap<>();
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();
        AppConfig appConfig = AppConfig.getInstance();
        String url = appConfig.getChatUrlStandby();
        url = appConfig.getChatUrl();
        String message = chatRequest.content();
        ChatRequestMessage requestMessage = ChatRequestMessage.create("user", message);
        List<ChatRequestMessage> messageList;
        if (ipChatMsgRecords.containsKey(chatRequest.ipAddress())) {
            messageList = ipChatMsgRecords.get(chatRequest.ipAddress());
            if (messageList.size() >= 10) {
                messageList.remove(0);
                messageList.remove(0);
            }
        } else {
            messageList = new ArrayList<>();
            ipChatMsgRecords.put(chatRequest.ipAddress(), messageList);
        }
        LogUtil.logInfo("chat server ip nums:%s,chat url:%s,chat messages size:%s", ipChatMsgRecords.size(),
                chatRequest.ipAddress(), messageList.size());

        messageList.add(requestMessage);

        //List<ChatRequestMessage> chatRequestMessages = Collections.singletonList(requestMessage);
        ChatRealRequest chatRealRequest = ChatRealRequest.create("gpt-3.5-turbo", messageList);

        String jsonBody = gson.toJson(chatRealRequest);
        LogUtil.logInfo("jsonBody:%s", jsonBody);
        Request request = new Request.Builder()
                .header(AUTHORIZATION_HEADER, "Bearer " + appConfig.getChatKey())
                .url(url)
                .post(okhttp3.RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LogUtil.logError("chat request error, response code:%d, message:%s", response.code(), response.message());
                throw new IOException("Unexpected code " + response);
            }
            String json = response.body().string();
            ChatRealResponse chatRealResponse = gson.fromJson(json, ChatRealResponse.class);
            LogUtil.logInfo("chatRealResponse:%s", chatRealResponse);

            long endTime = System.currentTimeMillis(); // 记录结束时间（毫秒）

            // 计算执行时间并转换为秒
            double elapsedTimeInSeconds = (endTime - startTime) / 1000.0; // 将毫秒转换为秒
            ChatResponse chatResponse = ChatResponse.create(chatRealResponse.choices().get(0).message().content(),
                    chatRequest.content(), System.currentTimeMillis(), elapsedTimeInSeconds, chatRequest.ipAddress());
            long saveId = dataManager.saveChat(chatResponse);
            LogUtil.logInfo("insert id:%s", saveId);

            messageList.add(ChatRequestMessage.create("assistant", chatResponse.content()));

            return chatResponse;
        } catch (IOException e) {
            LogUtil.logError("chat request IOException e:%s", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateToken(ChatToken tokens) {
        AppConfig appConfig = AppConfig.getInstance();
        String url = appConfig.getUpdateTokenUrl();
        String chatKey = tokens.tokens().get(0);
        LogUtil.logInfo("updateToken chatKey:%s", chatKey);
        appConfig.setChatKey(chatKey);
        String requestBody = gson.toJson(tokens.tokens());
        RequestBody body = RequestBody.create(requestBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Authorization", appConfig.getAuthorization())
                .patch(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LogUtil.logError("updateToken request error, response code:%d, message:%s", response.code(), response.message());
                throw new IOException("Unexpected code " + response);
            }
            LogUtil.logInfo("updateToken success ,value:%s ", tokens);
        } catch (IOException e) {
            LogUtil.logError("updateToken request IOException e:%s", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ChatResponse> getChatListToday() {
        return dataManager.getChatResponseToday();
    }

    @Override
    public ChatCount getChatListSizeToday() {
        return ChatCount.create(getChatListToday().size());
    }

    @Override
    public List<ChatResponse> getChatListHistory() {
        return dataManager.getAllChatResponse();
    }
}
