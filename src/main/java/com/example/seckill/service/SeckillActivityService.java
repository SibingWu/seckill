package com.example.seckill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    /**
     * 判断商品是否还有 redis 库存
     * @param activityId 秒杀活动ID
     * @return redis库存是否扣减成功
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidation(key);
    }

}
