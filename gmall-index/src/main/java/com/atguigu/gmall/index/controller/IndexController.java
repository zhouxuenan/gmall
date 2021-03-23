package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/xxx")
    @ResponseBody
    public String test(@RequestHeader("userId")String userId){
        return "获取到的用户登录信息：" + userId;
    }

    @GetMapping
    public String toIndex(Model model){

        List<CategoryEntity> categoryEntities = this.indexService.queryLvl1Categories();
        model.addAttribute("categories", categoryEntities);

        // TODO: 加载其他数据

        return "index";
    }

    @ResponseBody
    @GetMapping("index/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSub(@PathVariable("pid")Long pid){

        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesWithSub(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @ResponseBody
    @GetMapping("index/testlock")
    public ResponseVo<Object> testLock(){
        indexService.testLock();
        return ResponseVo.ok(null);
    }

    @ResponseBody
    @GetMapping("index/test/write")
    public ResponseVo<Object> testWrite(){
        indexService.testWrite();
        return ResponseVo.ok("写入成功！");
    }

    @ResponseBody
    @GetMapping("index/test/read")
    public ResponseVo<Object> testRead(){
        indexService.testRead();
        return ResponseVo.ok("读取成功！");
    }

    @ResponseBody
    @GetMapping("index/test/latch")
    public ResponseVo<Object> testLatch() throws InterruptedException {
        indexService.latch();
        return ResponseVo.ok("班长锁门成功。。。。。。");
    }

    @ResponseBody
    @GetMapping("index/test/countdown")
    public ResponseVo<Object> testCountdown(){
        indexService.countdown();
        return ResponseVo.ok("出来了一位同学。。。。。。");
    }

}