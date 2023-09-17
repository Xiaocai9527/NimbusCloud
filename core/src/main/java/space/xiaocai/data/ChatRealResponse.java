package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

import java.util.List;

@AutoValue
@GenerateTypeAdapter
public abstract class ChatRealResponse {
    public abstract String id();
    public abstract List<Choice> choices();
}
