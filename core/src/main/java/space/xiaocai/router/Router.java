package space.xiaocai.router;

import static space.xiaocai.api.Enums.RequestParams.REQUEST_BODY;
import static space.xiaocai.api.Enums.RequestParams.REQUEST_PATH;
import static space.xiaocai.api.Enums.RequestParams.REQUEST_QUERY;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.math3.util.Pair;

import com.google.gson.Gson;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;
import space.xiaocai.annotations.*;
import space.xiaocai.api.Enums;
import space.xiaocai.api.RequestParamParser;
import space.xiaocai.impl.IpAddressRequestParser;
import space.xiaocai.impl.SimpleRequestParser;
import space.xiaocai.util.Const;
import space.xiaocai.util.LogUtil;

/**
 * The router of Rest Api
 */
public class Router {
    // key 的值是 request_method+url,单独 url 可能没办法保证唯一性，因为有可能 get 和 post 使用同一个 url
    // 例如: GET_/rest/api/v1/chat/completions
    private final Map<String, Map<Enums.RequestMethod, MethodValue>> methodMap;
    private final Gson gson;

    public Router(Gson gson) {
        methodMap = new HashMap<>();
        this.gson = gson;
    }

    public void register(@Nonnull Object api) {
        Type[] genericInterfaces = api.getClass().getGenericInterfaces();
        if (genericInterfaces.length == 0) {
            LogUtil.logError("api:%s not implements interface");
            return;
        }
        Type genericInterface = genericInterfaces[0];
        Method[] methods = ((Class<?>) genericInterface).getDeclaredMethods();
        LogUtil.logInfo("methods size:%s", methods.length);
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequestMapper.class)) {
                RequestMapper annotation = method.getAnnotation(RequestMapper.class);
                Parameter[] parameters = method.getParameters();
                LogUtil.logInfo("parameters length:%s", parameters.length);
                List<Pair<Enums.RequestParams, Class<?>>> paramTypes = new ArrayList<>();
                Map<Enums.RequestMethod, MethodValue> methodValueMap = new HashMap<>();

                for (Parameter parameter : parameters) {
                    // 1. 我需要知道注解修饰的 type 类
                    // 2. 需要定一个枚举类型, 把注解和枚举一一对应起来
                    // 3. 我需要把这些 type 和对应的 注解一一对应起来,并且保存起来

                    // 因为我需要知道方法的参数， 参数的原始数据可以从 url 中解析出来, 但是需要知道参数的类型
                    // 参数的类型, 所以说需要把之前参数的类型保存到 methodValue 中。
                    // 因为是参数列表, 所以需要知道参数的类型列表 List<Class<?>> paramsTypes
                    // Class<?> 可以根据 parameter.getType() 得知
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        paramTypes.add(Pair.create(REQUEST_BODY, parameter.getType()));
                    } else if (parameter.isAnnotationPresent(RequestPath.class)) {
                        paramTypes.add(Pair.create(REQUEST_PATH, parameter.getType()));
                    } else if (parameter.isAnnotationPresent(RequestQuery.class)) {
                        paramTypes.add(Pair.create(REQUEST_QUERY, parameter.getType()));
                    }
                    LogUtil.logInfo("annotation method:%s", annotation.method());
                }
                methodValueMap.put(annotation.method(), MethodValue.create(method, api, paramTypes));
                String requestMethod = annotation.method().toString();
                LogUtil.logInfo("requestMethod:%s", requestMethod);
                methodMap.put(getKey(requestMethod, annotation.value()), methodValueMap);
            }
        }
        LogUtil.logInfo("methodMap:%s", methodMap);
    }

    public boolean filter(FullHttpRequest request) {
        String uri = request.uri();// /日志/server_log.log
        Map<Enums.RequestMethod, MethodValue> methodValueMap = methodMap.get(getKey(request.method().name(), uri));
        LogUtil.logInfo("methodMap:%s", methodMap);
        return !(methodValueMap != null && methodValueMap.containsKey(parseHttpMethod(request.method())));
    }

    private String getKey(String requestMethod, String uri) {
        return requestMethod + "_" + uri;
    }

    @Nullable
    public Object dispatch(FullHttpRequest request) {
        String uri = request.uri();// /日志/server_log.log
        Map<Enums.RequestMethod, MethodValue> methodValueMap = methodMap.get(getKey(request.method().name(), uri));
        MethodValue methodValue = methodValueMap.get(parseHttpMethod(request.method()));
        if (methodValue != null) {
            try {
                List<Object> params = parseParams(methodValue, request);
                return methodValue.method().invoke(methodValue.instance(), params.toArray(new Object[0]));
            } catch (IllegalAccessException | InvocationTargetException e) {
                LogUtil.logError("router dispatch error:%s", e);
            }
        }
        return null;
    }

    private List<Object> parseParams(MethodValue methodValue, FullHttpRequest request) {
        List<Object> params = new ArrayList<>();
        // 需要知道 method 的参数值
        List<Pair<Enums.RequestParams, Class<?>>> paramTypes = methodValue.parmaTypes();
        for (Pair<Enums.RequestParams, Class<?>> paramType : paramTypes) {
            Enums.RequestParams param = paramType.getKey();
            Class<?> typeValue = paramType.getValue();
            // 根据 param 区分是 body , path
            switch (param) {
                case REQUEST_BODY:
                    try {
                        String jsonString = request.content().toString(CharsetUtil.UTF_8);
                        LogUtil.logInfo("jsonString:%s", jsonString);
                        RequestParamParser<Object> paramParser = null;
                        if (typeValue.isAnnotationPresent(IPAddress.class)) {
                            paramParser = new IpAddressRequestParser(gson, typeValue, getRequestIp(request));
                        } else {
                            paramParser = new SimpleRequestParser<>(gson, typeValue);
                        }
                        params.add(paramParser.parse(jsonString));
                    } catch (Exception e) {
                        LogUtil.logError("gson request body error:%s", e);
                    }
                    break;
                case REQUEST_PATH:

                    break;
            }
        }
        return params;
    }

    private String getRequestIp(@Nonnull FullHttpRequest request) {
        if (request.headers().contains("X-Real-IP")) {
            return request.headers().get("X-Real-IP");
        }
        return Const.EMPTY;
    }

    private Enums.RequestMethod parseHttpMethod(HttpMethod httpMethod) {
        if (HttpMethod.GET.equals(httpMethod)) {
            return Enums.RequestMethod.GET;
        } else if (HttpMethod.POST.equals(httpMethod)) {
            return Enums.RequestMethod.POST;
        } else if (HttpMethod.PATCH.equals(httpMethod)) {
            return Enums.RequestMethod.PATCH;
        } else {
            return Enums.RequestMethod.GET;
        }
    }

}
