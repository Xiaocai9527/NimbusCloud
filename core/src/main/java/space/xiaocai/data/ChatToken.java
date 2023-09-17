package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

import java.util.List;

@AutoValue
@GenerateTypeAdapter
public abstract class ChatToken {
    public abstract List<String> tokens();
}
