package com.hme.data;

import com.alibaba.druid.pool.DruidDataSource;
import com.hme.data.domain.*;
import com.hme.data.utils.DBContextHolder;
import com.hme.data.utils.DynamicDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
class DynamicDatasourceApplicationTests {

    @Autowired
    private DynamicDataSource dynamicDataSource;
    public static String dynamicPrefix = "dymic";

    @Test
    void contextLoads() {
    }

    @Test
    void testdynamicData() {
        String dbName = dynamicPrefix + 1L; //your db id to identify your datasource
        try {
            boolean verifyDb = changeDb(dbName);
            if (verifyDb) {
                Map<Object, Object> dynamicDataMap = dynamicDataSource.getDynamicTargetDataSources();
                DruidDataSource druidDataSource = (DruidDataSource) dynamicDataMap.get(dbName);
                //do your logic code
                //List<Object> tableList = dbUtils.getTables(druidDataSource, sourceDBSchema, db_type);

                //change to main datasource
                DBContextHolder.clearDataSource();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean changeDb(String dbName) throws Exception {
        //change to main datasource
        DBContextHolder.clearDataSource();
        //get all setup datasource which is in used
        //List<db_connect_list> dataSourcesList = dbConnectListMapper.getDataInUse();
        List<db_connect_list> dataSourcesList = new ArrayList<>();

        for (db_connect_list dbConfig : dataSourcesList) {
            String curDb = dynamicPrefix + dbConfig.getDb_connect_id().toString();
            if (curDb.equals(dbName)) {
                System.out.println("we have already found datasource,dbName is:" + dbName);
                //create datasource and check the connection,if it has existed,just return
                dynamicDataSource.createDataSourceWithCheck(dbConfig);
                //change the datasource to target
                DBContextHolder.setDataSource(dbName);
                return true;
            }
        }
        return false;
    }

}
