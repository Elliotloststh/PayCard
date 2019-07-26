package yuan.paycard.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import yuan.paycard.model.PreItem;

import java.util.List;

@Mapper
public interface PreItemMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PreItem record);

    int insertSelective(PreItem record);

    PreItem selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PreItem record);

    int updateByPrimaryKey(PreItem record);

    @Select("select item_id from pre_item order by create_time desc limit #{limit}")
    List<Long> selectAll(Integer limit);
}