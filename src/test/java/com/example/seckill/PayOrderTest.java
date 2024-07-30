package com.example.seckill;

import com.example.seckill.service.SeckillActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PayOrderTest {

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Test
    public void payDoneTest() throws Exception {
//        seckillActivityService.payOrderProcess("1013282626817101824");
        seckillActivityService.payOrderProcess("");
    }
}
