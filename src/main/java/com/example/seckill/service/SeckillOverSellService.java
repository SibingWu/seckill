package com.example.seckill.service;

import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.po.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillOverSellService {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    // 这么写会导致高并发状态下超卖
    public String processSeckill(long activityId) {
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(activityId);
        int availableStock = activity.getAvailableStock();

        String result;

        if (availableStock > 0) {
            result = "恭喜，抢购成功！";
            System.out.println(result);

            availableStock--;
            activity.setAvailableStock(availableStock);
            seckillActivityDao.updateSeckillActivity(activity);
        } else {
            result = "抱歉，抢购失败，商品被抢完了……";
            System.out.println(result);
        }

        return result;
    }
}
