package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

@AutoValue
@GenerateTypeAdapter
public abstract class ChatCount {
    public abstract int count();

    public static ChatCount create(int count) {
        return new AutoValue_ChatCount(count);
    }
}
