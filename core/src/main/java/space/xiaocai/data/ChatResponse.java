package space.xiaocai.data;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class ChatResponse implements IpAddressOwner {
    public abstract String content();
    public abstract String title();
    public abstract long dateTime();
    public abstract double thinkTime();
    @Nullable
    public abstract String ipAddress();

    public static ChatResponse create(String content,
                                      String title,
                                      long dateTime,
                                      double thinkTime,
                                      String ipAddress) {
        return new AutoValue_ChatResponse(content, title, dateTime, thinkTime, ipAddress);
    }
}
