package com.example.wms.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 * 两个插件都是核心业务的地基：
 * 1. 分页插件 —— 所有列表查询走 Page 对象，插件自动拼 LIMIT 与 COUNT；
 * 2. 乐观锁插件 —— 库存表 inventory.version 带 @Version，
 *    updateById 时自动追加 "WHERE version = ?" 并 version+1，
 *    防止并发扣减库存互相覆盖（04 阶段出入库的核心并发保障）。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件必须指定数据库方言，否则无法生成正确的分页语句
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件：配合实体 @Version 字段使用
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
