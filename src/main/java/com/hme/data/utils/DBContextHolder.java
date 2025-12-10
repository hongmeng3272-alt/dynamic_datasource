package com.hme.data.utils;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hme
 * @create 2024-02-27-17:25
 */
@Slf4j
public class DBContextHolder {

    // safe for current thread
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();

    // change datasource
    public static void setDataSource(String dataSource) {
        contextHolder.set(dataSource);
        log.info("datasource has transfer to :{}",dataSource);
    }

    // 获取数据源
    public static String getDataSource() {
        return contextHolder.get();
    }

    // 删除数据源
    public static void clearDataSource() {
        contextHolder.remove();
        log.info("transfer to default datasource ");
    }
}
