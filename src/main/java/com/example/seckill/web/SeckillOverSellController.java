package com.example.seckill.web;

import com.example.seckill.service.SeckillActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeckillOverSellController {

//    @Autowired
//    private SeckillOverSellService seckillOverSellService;

    @Autowired
    private SeckillActivityService seckillActivityService;

//    /** 简单处理请求的方法：会导致高并发下超卖 */
//    @ResponseBody
//    @RequestMapping("/seckill/{seckillActivityId}")
//    public String seckill(@PathVariable("seckillActivityId") long seckillActivityId) {
//        return seckillOverSellService.processSeckill(seckillActivityId);
//    }

    /**
     * 使用 lua 脚本处理抢购请求
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable("seckillActivityId") long seckillActivityId) {
        // 仅扣减 redis 库存，未实际扣减数据库库存
        boolean stockValidationResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        return stockValidationResult ? "恭喜你秒杀成功！" : " 商品已售完，请下次再来……";
    }
}
