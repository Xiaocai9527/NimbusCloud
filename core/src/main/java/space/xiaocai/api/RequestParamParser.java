package space.xiaocai.api;

public interface RequestParamParser<T> {
    T parse(String json);
}
