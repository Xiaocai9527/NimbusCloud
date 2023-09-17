package space.xiaocai.impl;

import com.google.gson.Gson;
import space.xiaocai.api.RequestParamParser;

import javax.annotation.Nonnull;

public class SimpleRequestParser<C> implements RequestParamParser<Object> {

    protected final Gson gson;
    protected final Class<?> typeValue;

    public SimpleRequestParser(@Nonnull Gson gson, @Nonnull Class<?> typeValue) {
        this.gson = gson;
        this.typeValue = typeValue;
    }

    @Override
    public Object parse(String json) {
        return gson.fromJson(json, typeValue);
    }
}
