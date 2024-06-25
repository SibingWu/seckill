package com.example.seckill.web;

import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.dao.SeckillCommodityDao;
import com.example.seckill.db.po.Order;
import com.example.seckill.db.po.SeckillActivity;
import com.example.seckill.db.po.SeckillCommodity;
import com.example.seckill.service.SeckillActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class SeckillActivityController {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private SeckillActivityService seckillActivityService;

    // 发布页面
    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

    // 提交发布
//    @ResponseBody
    // add_activity.html 中用到 <form th:action="@{/addSeckillActivityAction}" method="post">
    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber, // add_activity.html 中<input type="text" name="seckillNumber">
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap // 把后端数据带到前段
    ) throws ParseException {
        startTime = startTime.substring(0, 10) + startTime.substring(11);
        endTime = endTime.substring(0, 10) + endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");

        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(Integer.parseInt("" + seckillNumber)); // long 转 int
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));

        // 插入数据库
        seckillActivityDao.insertSeckillActivity(seckillActivity);

        resultMap.put("seckillActivity", seckillActivity); // add_success.html 中用到 <span th:text="${seckillActivity.name}">

        return "add_success";
    }

    @RequestMapping("/seckills")
    public String activityList(
            Map<String, Object> resultMap
    ) {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
        resultMap.put("seckillActivities", seckillActivities); // seckill_activity.html 中用到 <tr th:each="seckillActivity : ${seckillActivities}">
        return "seckill_activity";
    }

    // seckill_activity.html 中用到 <a class='sui-btn btn-block btn-buy'  th:href="@{'/item/'+${seckillActivity.id}}" target='_blank'>立即抢购</a>
    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(
            @PathVariable("seckillActivityId") long seckillActivityId,
            Map<String, Object> resultMap
    ) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());

        resultMap.put("seckillActivity", seckillActivity); // seckill_item.html 中用到 <em th:text="'￥'+${seckillActivity.seckillPrice}"></em>
        resultMap.put("seckillCommodity", seckillCommodity); // seckill_item.html 中用到 <h4 th:text="${seckillCommodity.commodityName}"></h4>

//        // 另一种写法
//        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
//        resultMap.put("oldPrice", seckillActivity.getOldPrice());
//        resultMap.put("commodityId", seckillActivity.getCommodityId());
//        resultMap.put("commodityName", seckillCommodity.getCommodityName());
//        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());

        return "seckill_item";
    }

    /**
     * 处理抢购请求
     *
     * @param userId
     * @param seckillActivityId
     * @return
     */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable("userId") long userId, @PathVariable("seckillActivityId") long seckillActivityId) {
        boolean stockValidateResult = false;

        ModelAndView modelAndView = new ModelAndView();

        try {
            // 确认是否能够进行秒杀
            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId); // 通过 lua 脚本判断库存是否充足
            if (stockValidateResult) {
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                // seckill_result.html 中用到 <em th:text="${resultInfo}"></em>
                modelAndView.addObject("resultInfo", "秒杀成功，订单创建中，订单ID:" + order.getOrderNo());
                // seckill_result.html 中用到 <a class="sui-btn btn-danger btn-xlarge" th:href="@{'/seckill/orderQuery/'+ ${orderNo}}"
                modelAndView.addObject("orderNo", order.getOrderNo());
            } else {
                modelAndView.addObject("resultInfo", "对不起，商品库存不足");
            }
        } catch (Exception e) {
            log.error("秒杀系统异常: " + e);
            modelAndView.addObject("resultInfo", "秒杀失败");
        }
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }
}
