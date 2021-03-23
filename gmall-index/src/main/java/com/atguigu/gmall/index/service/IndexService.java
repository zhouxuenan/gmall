package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.lock.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    public static final String KEY_PREFIX = "index:category:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(0l);
        return listResponseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 129600l,random = 14400,lock = "lock:cates:")
    public List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    public List<CategoryEntity> queryLvl2CategoriesWithSub2(Long pid) {
        //从缓存中获取
        String cacheCategories = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(StringUtils.isNotBlank(cacheCategories)){
            //如果缓存中有，直接返回
            return JSON.parseArray(cacheCategories, CategoryEntity.class);
        }
        RLock lock = this.redissonClient.getLock("lock:" + pid);
        lock.lock();
        try {
            //加锁过程中可能已经有其他线程把数据放入缓存，再去检查缓存
            String cacheCategories2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if(StringUtils.isNotBlank(cacheCategories2)){
                //如果缓存中有，直接返回
                return JSON.parseArray(cacheCategories2, CategoryEntity.class);
            }

            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();

            if(CollectionUtils.isEmpty(categoryEntities)){
                //为了防止缓存穿透，数据即使为null也缓存，为了防止缓存数据过多，缓存时间设置的极短
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities),1,TimeUnit.MINUTES);
            }else {
                //把查询结果放入缓存
                //为了防止缓存雪崩，给缓存时间添加随机值
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities),2160 + new Random().nextInt(360), TimeUnit.HOURS);
            }
            return categoryEntities;
        } finally {
            lock.unlock();
        }
    }

//    public synchronized void testLock() {
//        // 查询redis中的num值
//        String value = this.redisTemplate.opsForValue().get("num");
//        // 没有该值return
//        if (StringUtils.isBlank(value)){
//            return ;
//        }
//        // 有值就转成成int
//        int num = Integer.parseInt(value);
//        // 把redis中的num值+1
//        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
//    }

    public void testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        try {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        } finally {
            lock.unlock();
        }
    }

    public void testLock2() {
        // 1. 从redis中获取锁,setnx
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);
        if (lock) {
            // 读取redis中的num值
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            try {
                TimeUnit.SECONDS.sleep(180);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        // 测试可重入性
        this.testSub("lock", uuid);
        // 释放锁
        this.distributedLock.unlock("lock", uuid);
    }

    // 测试可重入性
    private void testSub(String lockName,String uuid){
        this.distributedLock.tryLock(lockName, uuid, 30);
        System.out.println("测试可重入的分布式锁");
        this.distributedLock.unlock(lockName, uuid);
    }

    public void testLock1() {
        // 1. 从redis中获取锁,setnx
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", "uuid",3,TimeUnit.SECONDS);
        if (lock) {
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)){
                return ;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2. 释放锁 del
//            if(StringUtils.equals(redisTemplate.opsForValue().get("lock"), uuid)){
//                this.redisTemplate.delete("lock");
//            }
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"), uuid);
        } else {
            // 3. 每隔1秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);
        System.out.println("模仿了写的操作。。。。。。");
        //TODO:释放锁
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);
        System.out.println("模仿了读的操作。。。。。。");
        //TODO:释放锁
    }

    public void latch() throws InterruptedException {
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("latch");
        cdl.trySetCount(6);
        cdl.await();
    }

    public void countdown() {
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("latch");
        cdl.countDown();
    }
}