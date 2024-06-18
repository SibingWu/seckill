package com.example.seckill;

import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.mappers.SeckillActivityMapper;
import com.example.seckill.db.po.SeckillActivity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class DaoTest {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Test
    void SeckillActivityTest() {
        // Arrange
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName("测试");
        seckillActivity.setCommodityId(999L);
        seckillActivity.setTotalStock(1005L);
        seckillActivity.setSeckillPrice(new BigDecimal("99"));
        seckillActivity.setActivityStatus(16);
        seckillActivity.setOldPrice(new BigDecimal(99));
        seckillActivity.setAvailableStock(100);
        seckillActivity.setLockStock(0L);

        // Act
        seckillActivityMapper.insert(seckillActivity);

        // Assert
        System.out.println("====>>>>" + seckillActivityMapper.selectByPrimaryKey(1L));
    }

    @Test
    void setSeckillActivityQuery() {
        // Act
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(0);

        // Assert
        System.out.println(seckillActivities.size());
        seckillActivities.stream().forEach(seckillActivity -> System.out.println(seckillActivity.toString()));
    }
}
