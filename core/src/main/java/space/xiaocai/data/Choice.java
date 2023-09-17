package space.xiaocai.data;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

@AutoValue
@GenerateTypeAdapter
public abstract class Choice {
    public abstract int index();
    public abstract ChoiceMessage message();

}
