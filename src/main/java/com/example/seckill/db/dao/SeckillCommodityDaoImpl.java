package com.example.seckill.db.dao;

import com.example.seckill.db.mappers.SeckillCommodityMapper;
import com.example.seckill.db.po.SeckillCommodity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SeckillCommodityDaoImpl implements SeckillCommodityDao {

    @Autowired
    private SeckillCommodityMapper seckillCommodityMapper;

    @Override
    public SeckillCommodity querySeckillCommodityById(long commodityId) {
        return seckillCommodityMapper.selectByPrimaryKey(commodityId);
    }
}
