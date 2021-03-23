package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    /**
     * 缓存的前缀
     * 结构：模块名+':'+实例名+':'
     * 例如首页工程三级分类缓存
     * @return
     */
    String prefix() default "gmall:cache:";

    /**
     * 设置缓存的有效时间
     * 单位：分钟
     * @return
     */
    long timeout() default 5l;

    /**
     * 防止雪崩设置的随机值范围
     * @return
     */
    int random() default 5;

    /**
     * 防止击穿，给缓存添加分布式锁
     * 这里指定分布式锁的前缀
     * 最终分布式锁名称：lock + 方法参数
     * @return
     */
    String lock() default "lock:";
}
