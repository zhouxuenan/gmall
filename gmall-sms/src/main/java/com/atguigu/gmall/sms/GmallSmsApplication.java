package com.atguigu.gmall.sms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.sms.mapper")
@EnableSwagger2
@RefreshScope
@EnableFeignClients
public class GmallSmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallSmsApplication.class, args);
    }

}
