package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

@AutoValue
@GenerateTypeAdapter
public abstract class ChatRequestMessage {
    public abstract String role();

    public abstract String content();

    public static ChatRequestMessage create(String role,
                                            String content) {
        return new AutoValue_ChatRequestMessage(role, content);
    }
}
