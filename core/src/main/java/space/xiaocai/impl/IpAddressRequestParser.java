package space.xiaocai.impl;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import space.xiaocai.data.ChatRequest;
import space.xiaocai.data.ChatResponse;
import space.xiaocai.data.IpAddressOwner;

public class IpAddressRequestParser extends SimpleRequestParser<IpAddressOwner> {

    private final String ipAddress;

    public IpAddressRequestParser(@NotNull Gson gson, @NotNull Class<?> typeValue, String ipAddress) {
        super(gson, typeValue);
        this.ipAddress = ipAddress;
    }

    @Override
    public IpAddressOwner parse(String json) {
        if (typeValue.isAssignableFrom(ChatResponse.class)) {
            ChatRequest chatRequest = (ChatRequest) gson.fromJson(json, typeValue);
            chatRequest = ChatRequest.create(chatRequest.content(), ipAddress);
            return chatRequest;
        }

        return ((IpAddressOwner) super.parse(json));
    }
}
