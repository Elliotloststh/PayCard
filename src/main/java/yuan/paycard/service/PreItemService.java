package yuan.paycard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuan.paycard.dao.PreItemMapper;
import yuan.paycard.model.PreItem;

import java.util.List;

@Service
public class PreItemService {
    @Autowired
    PreItemMapper preItemMapper;

    public List<Long> selectAll(Integer limit) {
        return preItemMapper.selectAll(limit);
    }
}
