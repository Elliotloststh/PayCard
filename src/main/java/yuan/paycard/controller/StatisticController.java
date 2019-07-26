package yuan.paycard.controller;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yuan.paycard.annotation.UserLoginToken;
import yuan.paycard.model.Item;
import yuan.paycard.model.Order;
import yuan.paycard.service.AccountService;
import yuan.paycard.service.FamilyRelationService;
import yuan.paycard.service.ItemService;
import yuan.paycard.service.OrderService;
import yuan.paycard.utils.ApiResponse;
import yuan.paycard.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistic")
@Api(value = "统计报表Controller")
public class StatisticController {
    @Autowired
    AccountService accountService;

    @Autowired
    OrderService orderService;

    @Autowired
    FamilyRelationService familyRelationService;

    @Autowired
    ItemService itemService;

    @UserLoginToken
    @GetMapping("/histogram/oneWeek")
    public ApiResponse getOneWeekHistogram(HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        Date now = new Date();
        List<DailyStatistic> statistics = genWeekHistogram(userId,CommonUtils.getWeekBegin(now), CommonUtils.getWeekEnd(now));
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("statistics", statistics);
        return response;
    }

    private List<DailyStatistic> genWeekHistogram(long userId,long start, long end) {
        long dayStart = start;
        long dayEnd = dayStart + 24*60*60 -1;
        List<DailyStatistic> statistics = new ArrayList<>();
        while(dayEnd <= end) {
            statistics.add(genDailyHistogram(userId, dayStart, dayEnd));
            dayStart = dayEnd+1;
            dayEnd = dayStart + 24*60*60 -1;
        }
        return statistics;
    }

    private DailyStatistic genDailyHistogram(long userId, long start, long end) {
        List<Order> orders = orderService.queryDailyOrder(userId, start, end);
        double sum = 0;
        for(Order order:orders) {
            sum += order.getPayment();
        }
        BigDecimal bg = new BigDecimal(sum).setScale(2, RoundingMode.UP);
        return new DailyStatistic(orders.size(), bg.doubleValue());
    }

    @UserLoginToken
    @GetMapping("/pieChart/oneWeek")
    public ApiResponse getOneWeekPieChart(HttpServletRequest request) {
        long userId = CommonUtils.getUserId(request);
        Date now = new Date();
        long startTime = CommonUtils.getWeekBegin(now);
        long endTime = CommonUtils.getWeekEnd(now);
        List<Order> orders = orderService.queryDailyOrder(userId, startTime, endTime);
        List<OrderWithItem> orderWithItemList = new ArrayList<>();
        for(Order order:orders) {
            OrderWithItem one = new OrderWithItem();
            one.setOrder(order);
            List<Item> items = converStringToList(order.getItemId()).stream().map(id-> itemService.selectById(id)).collect(Collectors.toList());
            one.setItems(items);
            orderWithItemList.add(one);
        }

        double sum = 0;

        Map<String, Double> map = new HashMap<>();
        for(OrderWithItem one: orderWithItemList) {
            sum += one.getOrder().getPayment();
            for(Item item: one.getItems()) {
                String catalog = item.getCatalog();
                double payment = item.getPrice();

                if(!map.containsKey(catalog)) {
                    map.put(catalog, payment);
                } else {
                    double origin = map.get(catalog);
                    map.put(catalog, payment+origin);
                }
            }
        }

        List<pieChartOutput> output = new ArrayList<>();
        for(String key:map.keySet()) {
            pieChartOutput one = new pieChartOutput();
            one.setCatalog(key);
            one.setAmount(CommonUtils.round(map.get(key), 2));
            one.setPercentage(CommonUtils.round(map.get(key)/sum, 4));
            output.add(one);
        }
        ApiResponse response = ApiResponse.ok();
        response.putDataValue("statistics", output);
        return response;
    }

    @Data
    @AllArgsConstructor
    static class DailyStatistic {
        Integer count;
        Double amount;
    }

    @Data
    static class OrderWithItem {
        Order order;
        List<Item> items;
    }

    @Data
    static class pieChartOutput {
        String catalog;
        Double amount;
        Double percentage;
    }

    private List<Long> converStringToList(String strs) {
        if (strs!=null) {
            String[] idStrs = strs.trim().split(",");
            if (idStrs.length > 0) {
                List<Long> strsList = new ArrayList<>();
                for (String str : idStrs) {
                    if (str!=null) {
                        strsList.add(Long.parseLong(str.trim()));
                    }
                }
                if (strsList.size() > 0) {
                    return strsList;
                }
            }
        }
        return null;
    }

}
