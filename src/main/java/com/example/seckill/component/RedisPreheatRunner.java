package com.example.seckill.component;

import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.po.SeckillActivity;
import com.example.seckill.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 启动项目时向 Redis 存入商品库存 */
@Component
public class RedisPreheatRunner implements ApplicationRunner {

    @Autowired
    private SeckillActivityDao seckillActivityDao; // 用于从数据库中读取库存信息

    @Autowired
    private RedisService redisService; // 用于将数据库中读取的信息注入到 redis

    /**
     * 启动项目时向 Redis 存入商品库存
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
        for (SeckillActivity seckillActivity: seckillActivities) {
            String key = "stock:" + seckillActivity.getId();
            long stock = (long) seckillActivity.getAvailableStock();
            redisService.setValue(key, stock);
        }
    }
}
