package yuan.paycard.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import yuan.paycard.model.FamilyRelation;

import java.util.List;

@Mapper
public interface FamilyRelationMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FamilyRelation record);

    int insertSelective(FamilyRelation record);

    FamilyRelation selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(FamilyRelation record);

    int updateByPrimaryKey(FamilyRelation record);

    @Select("select * from family_relation where family_id=#{familyId} and (status=0 or status=2)")
    FamilyRelation findRelation(Long familyId);

    @Select("select * from family_relation where guardian_id=#{guardianId} and status!=1")
    List<FamilyRelation> getAllRelation(Long guardianId);
}