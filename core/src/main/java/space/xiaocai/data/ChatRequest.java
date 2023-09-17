package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import space.xiaocai.annotations.IPAddress;

import javax.annotation.Nullable;

@IPAddress
@AutoValue
@GenerateTypeAdapter
public abstract class ChatRequest implements IpAddressOwner{
    public abstract String content();
    @Nullable
    public abstract String ipAddress();

    public static ChatRequest create(String content,
                                     @Nullable String ipAddress) {
        return new AutoValue_ChatRequest(content, ipAddress);
    }
}
