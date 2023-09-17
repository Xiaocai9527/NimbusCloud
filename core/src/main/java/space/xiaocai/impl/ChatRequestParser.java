package space.xiaocai.impl;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import space.xiaocai.data.ChatRequest;

public class ChatRequestParser extends SimpleRequestParser<ChatRequest> {

    private final String ip;

    public ChatRequestParser(@NotNull Gson gson, String ip) {
        super(gson, ChatRequest.class);
        this.ip = ip;
    }

    @Override
    public ChatRequest parse(String json) {
        ChatRequest chatRequest = (ChatRequest) gson.fromJson(json, typeValue);
        chatRequest = ChatRequest.create(chatRequest.content(), ip);
        return chatRequest;
    }
}
