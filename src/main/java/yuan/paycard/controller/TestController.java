package yuan.paycard.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.SocketIOClient;
import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yuan.paycard.model.Item;
import yuan.paycard.model.PayMode;
import yuan.paycard.service.ItemService;
import yuan.paycard.service.OrderService;
import yuan.paycard.service.PayModeService;
import yuan.paycard.service.PushService;
import yuan.paycard.socket.ClientCache;
import yuan.paycard.utils.ApiResponse;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@Api(value="测试Controller")
public class TestController {
    @Autowired
    OrderService orderService;

    @Autowired
    ItemService itemService;

    @Autowired
    PushService pushService;

    @Resource
    private ClientCache clientCache;

    @Autowired
    PayModeService payModeService;

    @PostMapping("/sendOrder")
    public ApiResponse sendOrder(@RequestBody SendOrderInput input) {
        List<Item> item = input.getItemIds().stream().map(id->itemService.selectById(id)).collect(Collectors.toList());

        List<JSONObject> itemJson = item.stream().map(one-> (JSONObject)JSON.toJSON(one)).collect(Collectors.toList());
        JSONObject contentJson = new JSONObject();
        contentJson.put("item", itemJson);
        contentJson.put("sellerId", input.getSellerId());
        contentJson.put("payment", input.getPayment());
        contentJson.put("paymentType", input.getPaymentType());
        pushService.sendMessageToAlias("NewOrder",contentJson.toJSONString(), String.valueOf(input.getUserId()));
        return ApiResponse.ok();
    }

    @PostMapping("/deposit")
    public ApiResponse deposit(@RequestBody depositInput input) {
        PayMode payMode = payModeService.selectById(input.getPayModeId());
        payMode.setAmount(payMode.getAmount()+input.getMoney());
        payModeService.update(payMode);
        return ApiResponse.ok();
    }

    @GetMapping("/user/{userId}")
    public String pushToUser(@PathVariable("userId") String userId){
        HashMap<UUID, SocketIOClient> userClient = clientCache.getUserClient(userId);
        userClient.forEach((uuid, socketIOClient) -> {
            //向客户端推送消息
            socketIOClient.sendEvent("test","测试消息");
        });
        return "success";
    }


    @Data
    static class SendOrderInput {
        private List<Long> itemIds;
        private Long sellerId;
        private Double payment;
        private String paymentType;
        private Long userId;
    }

    @Data
    static class depositInput {
        Long payModeId;
        Long money;
    }
}
