package space.xiaocai.api;

import java.util.List;

import space.xiaocai.annotations.RequestBody;
import space.xiaocai.annotations.RequestMapper;
import space.xiaocai.data.ChatCount;
import space.xiaocai.data.ChatRequest;
import space.xiaocai.data.ChatResponse;
import space.xiaocai.data.ChatToken;

public interface ChatApi {

    @RequestMapper(value = "/rest/api/v1/chat/completions", method = Enums.RequestMethod.POST)
    ChatResponse chat(@RequestBody ChatRequest chatRequest);

    @RequestMapper(value = "/rest/api/v1/admin/tokens", method = Enums.RequestMethod.POST)
    void updateToken(@RequestBody ChatToken tokens);

    @RequestMapper(value = "/rest/api/v1/todayChatList")
    List<ChatResponse> getChatListToday();

    @RequestMapper(value = "/rest/api/v1/todayChatListSize")
    ChatCount getChatListSizeToday();

    @RequestMapper(value = "/rest/api/v1/historyChatList")
    List<ChatResponse> getChatListHistory();
}
