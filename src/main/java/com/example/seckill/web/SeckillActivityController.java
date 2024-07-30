package com.example.seckill.web;

import com.alibaba.fastjson.JSON;
import com.example.seckill.db.dao.OrderDao;
import com.example.seckill.db.dao.SeckillActivityDao;
import com.example.seckill.db.dao.SeckillCommodityDao;
import com.example.seckill.db.po.Order;
import com.example.seckill.db.po.SeckillActivity;
import com.example.seckill.db.po.SeckillCommodity;
import com.example.seckill.service.RedisService;
import com.example.seckill.service.SeckillActivityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private RedisService redisService;

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
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;

        String seckillActivityInfo = redisService.getValue("seckillActivity:" + seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityInfo)) {
            log.info("redis缓存数据:" + seckillActivityInfo);
            seckillActivity = JSON.parseObject(seckillActivityInfo, SeckillActivity.class);
        } else {
            seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }

        String seckillCommodityInfo = redisService.getValue("seckillCommodity:" + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityInfo)) {
            log.info("redis缓存数据:" + seckillCommodityInfo);
            seckillCommodity = JSON.parseObject(seckillCommodityInfo, SeckillCommodity.class);
        } else {
            seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivityId);
        }

        resultMap.put("seckillActivity", seckillActivity); // seckill_item.html 中用到 <em th:text="'￥'+${seckillActivity.seckillPrice}"></em>
        resultMap.put("seckillCommodity", seckillCommodity); // seckill_item.html 中用到 <h4 th:text="${seckillCommodity.commodityName}"></h4>

//        // 另一种写法
//        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
//        resultMap.put("oldPrice", seckillActivity.getOldPrice());
//        resultMap.put("commodityId", seckillActivity.getCommodityId());
//        resultMap.put("commodityName", seckillCommodity.getCommodityName());
//        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());

        /*
         * 这个方法的主要作用是从数据库中查询秒杀活动和商品信息，并将这些信息存储在 resultMap 中，然后返回视图名称 "seckill_item"。
         * Spring MVC 会自动将 resultMap 中的数据传递给视图渲染器。
         * 由于不需要在处理过程中进行复杂的逻辑处理或者动态调整视图名称，因此直接返回视图名称是简单且有效的做法。
         */
        return "seckill_item";
    }

    /**
     * 处理抢购请求
     *
     * @param userId
     * @param seckillActivityId
     * @return
     */
    // seckill_item.html 中用到 <a th:href="'/seckill/buy/1234/' + ${seckillActivity.id}" target="_blank"
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable("userId") long userId, @PathVariable("seckillActivityId") long seckillActivityId) {
        boolean stockValidateResult = false;

        ModelAndView modelAndView = new ModelAndView();

        try {
            // 判断用户是否在已购名单中
            if (redisService.isInLimitMember(seckillActivityId, userId)) {
                // 提示用户已经在限购名单中，返回结果
                modelAndView.addObject("resultInfo", "对不起，您已经在限购名单中");
                modelAndView.setViewName("seckill_result");
                return modelAndView;
            }

            // 确认是否能够进行秒杀
            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId); // 通过 lua 脚本判断库存是否充足
            if (stockValidateResult) {
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                // seckill_result.html 中用到 <em th:text="${resultInfo}"></em>
                modelAndView.addObject("resultInfo", "秒杀成功，订单创建中，订单ID:" + order.getOrderNo());
                // seckill_result.html 中用到 <a class="sui-btn btn-danger btn-xlarge" th:href="@{'/seckill/orderQuery/'+ ${orderNo}}"
                modelAndView.addObject("orderNo", order.getOrderNo());

                // 添加用户到已购名单中
                redisService.addLimitMember(seckillActivityId, userId);
            } else {
                modelAndView.addObject("resultInfo", "对不起，商品库存不足");
            }
        } catch (Exception e) {
            log.error("秒杀系统异常: " + e);
            modelAndView.addObject("resultInfo", "秒杀失败");
        }

        /*
         * 这个方法的处理逻辑更为复杂。它不仅需要验证库存，还要处理订单创建，并根据不同的情况设置不同的视图数据。
         * ModelAndView 对象允许你在一个对象中同时设置视图名称和视图数据，并在处理过程中灵活地修改它们。这在以下场景中特别有用：
         * 1. 动态调整视图名称：如果你的业务逻辑需要根据不同的条件返回不同的视图，可以使用 modelAndView.setViewName() 来动态设置视图名称。
         * 2. 丰富的视图数据：你可以通过 modelAndView.addObject() 方法在处理过程中动态添加多个视图数据。
         */
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    // seckill_result.html 中用到 <a class="sui-btn btn-danger btn-xlarge" th:href="@{'/seckill/orderQuery/'+ ${orderNo}}"
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView seckillOrderQuery(@PathVariable("orderNo") String orderNo) {
        log.info("订单查询，订单号： " + orderNo);

        ModelAndView modelAndView = new ModelAndView();

        Order order = orderDao.queryOrder(orderNo);
        if (order != null) {
            modelAndView.setViewName("order"); // order.html
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order_wait"); // 本 project 未实现
        }

        return modelAndView;
    }

    // order.html 中用到 <a class="sui-btn btn-danger btn-xlarge" th:href="@{'/seckill/payOrder/' + ${order.orderNo}}">支付订单金额</a>
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(@PathVariable("orderNo") String orderNo) throws Exception {
        seckillActivityService.payOrderProcess(orderNo);
        /*
         * 在 payOrder 方法中使用 redirect 是为了在完成支付处理后重定向到另一个页面。这种做法有几个主要原因：
         * 1. 清晰的请求流：
         * 使用 redirect 可以清晰地将用户的请求从当前页面重定向到新的 URL。这种方式对于用户体验来说是自然的，因为用户的浏览器地址栏会显示新的 URL。
         *
         * 2. 防止表单重复提交：
         * 在进行支付等操作时，使用 redirect 可以避免表单重复提交的问题。通过重定向，浏览器会向新的 URL 发送一个新的 GET 请求，而不是重复提交表单数据。
         *
         * 3. 保持请求和响应的分离：
         * 使用 redirect 可以将处理逻辑和结果展示分开，提高代码的清晰度和可维护性。处理逻辑在服务层完成，而最终结果的展示通过重定向到其他页面来完成。
         */
        return "redirect:/seckill/orderQuery/" + orderNo;
    }

    /**
     * 获取当前服务器端时间
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/getSystemTime")
    public String getSystemTime() {
        // 设置日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // new Date() 为获取当前系统时间
        String date = sdf.format(new Date());
        return date;
    }
}
