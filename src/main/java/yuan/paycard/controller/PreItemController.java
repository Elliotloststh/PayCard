package yuan.paycard.controller;

import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yuan.paycard.model.Item;
import yuan.paycard.model.PreItem;
import yuan.paycard.service.ItemService;
import yuan.paycard.service.PreItemService;
import yuan.paycard.utils.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/preItem")
@Api(value = "预置商品Controller")
public class PreItemController {
    @Autowired
    PreItemService preItemService;
    @Autowired
    ItemService itemService;

    @GetMapping("/get/{size}")
    public ApiResponse getAll(@PathVariable("size") Integer size) {
        List<Long> preItems = preItemService.selectAll(size);
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("items", preItems.stream().map(itemService::selectById).collect(Collectors.toList()));
        return response;
    }

//    @Data
//    static class PreItemOutput {
//        private Long id;
//
//        private String title;
//
//        private String cover;
//
//        private String catalog;
//
//        private Double price;
//
//        private Long createTime;
//
//        private Long sellerId;
//
//        private String paymentType;
//    }
}
