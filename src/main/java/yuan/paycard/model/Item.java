package yuan.paycard.model;

import lombok.Data;

@Data
public class Item {
    private Long id;

    private String title;

    private String cover;

    private String catalog;

    private Double price;

    private Long createTime;
}