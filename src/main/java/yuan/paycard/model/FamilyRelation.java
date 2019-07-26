package yuan.paycard.model;

import lombok.Data;

@Data
public class FamilyRelation {
    private Long id;

    private Long guardianId;

    private Long familyId;

    private String relationName;

    private Double monthLimit;

    private Double singleLimit;

    private Integer status;

    private Long createTime;

    private Long updateTime;

}