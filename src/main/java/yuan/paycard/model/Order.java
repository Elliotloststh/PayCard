package yuan.paycard.model;

import lombok.Data;

@Data
public class Order {
    private Long id;

    private String itemId;

    private Long payerId;

    private Long actualPayerId;

    private Long sellerId;

    private Double payment;

    private String paymentType;

    private Integer source;

    private Integer status;

    private String url;

    private Long createTime;

    private Long updateTime;
}