package com.example.wms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WMS 后端启动类。
 * MapperScan：批量注册 mapper 包下的接口为 MyBatis 映射 bean；
 * EnableScheduling：开启定时任务支持（05 阶段的预警全量扫描依赖它）；
 * EnableAsync：开启异步方法支持（操作日志切面异步落库、05 阶段事件监听异步处理）。
 */
@SpringBootApplication
@MapperScan("com.example.wms.mapper")
@EnableScheduling
@EnableAsync
public class WmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsApplication.class, args);
    }
}
