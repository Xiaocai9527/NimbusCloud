package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

import java.util.List;

@AutoValue
@GenerateTypeAdapter
public abstract class ChatRealRequest {
    public abstract String model();
    public abstract List<ChatRequestMessage> messages();

    public static ChatRealRequest create(String model,
                                         List<ChatRequestMessage> messages) {
        return new AutoValue_ChatRealRequest(model, messages);
    }
}
