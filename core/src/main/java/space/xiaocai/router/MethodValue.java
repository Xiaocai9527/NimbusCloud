package space.xiaocai.router;

import com.google.auto.value.AutoValue;
import org.apache.commons.math3.util.Pair;
import space.xiaocai.api.Enums;

import java.lang.reflect.Method;
import java.util.List;

@AutoValue
public abstract class MethodValue {
    public abstract Method method();
    public abstract Object instance();
    public abstract List<Pair<Enums.RequestParams, Class<?>>> parmaTypes();

    public static MethodValue create(Method method, Object instance, List<Pair<Enums.RequestParams, Class<?>>> parmaTypes) {
        return new AutoValue_MethodValue(method, instance, parmaTypes);
    }
}
