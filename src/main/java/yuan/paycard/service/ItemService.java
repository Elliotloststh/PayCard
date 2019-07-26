package yuan.paycard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yuan.paycard.dao.ItemMapper;
import yuan.paycard.model.Item;

@Service
public class ItemService {
    @Autowired
    ItemMapper itemMapper;

    public Item selectById(long id) {
        return itemMapper.selectByPrimaryKey(id);
    }
}
