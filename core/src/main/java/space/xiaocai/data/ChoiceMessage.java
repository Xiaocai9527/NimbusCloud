package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

@AutoValue
@GenerateTypeAdapter
public abstract class ChoiceMessage {
    public abstract String role();
    public abstract String content();
}
