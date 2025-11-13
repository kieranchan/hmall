package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    @Override
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";
        boolean r = false;
        try {
            r = executeBatch(items, new BiConsumer<SqlSession, OrderDetailDTO>() {
                @Override
                public void accept(SqlSession sqlSession, OrderDetailDTO entity) {
                    sqlSession.update(sqlStatement, entity);
                }
            });
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    @Override
    public void restockItemByItemIdsAndNums(List<OrderDetailDTO> items) {
        // 根據商品id和數量恢復庫存
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.restockItemByItemIdsAndNums";
        boolean r = false;
        try {
            r = executeBatch(items, new BiConsumer<SqlSession, OrderDetailDTO>() {
                @Override
                public void accept(SqlSession sqlSession, OrderDetailDTO entity) {
                    sqlSession.update(sqlStatement, entity);
                }
            });
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }
}