package yuan.paycard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuan.paycard.dao.FamilyRelationMapper;
import yuan.paycard.model.FamilyRelation;

import java.util.List;

@Service
public class FamilyRelationService {
    @Autowired
    FamilyRelationMapper familyRelationMapper;

    public FamilyRelation findRelation(Long familyId) {
        return familyRelationMapper.findRelation(familyId);
    }

    public List<FamilyRelation> getAllRelation(Long guardianId) {
        return familyRelationMapper.getAllRelation(guardianId);
    }

    public FamilyRelation selectById(Long relationId) {
        return familyRelationMapper.selectByPrimaryKey(relationId);
    }

    public int updateSelective(FamilyRelation record) {
        return familyRelationMapper.updateByPrimaryKeySelective(record);
    }

    public int insert(FamilyRelation relation) {
        return familyRelationMapper.insertSelective(relation);
    }
}
