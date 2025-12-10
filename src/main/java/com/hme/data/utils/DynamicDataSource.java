package com.hme.data.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;
import com.hme.data.domain.db_connect_list;
import lombok.extern.slf4j.Slf4j;


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Set;

/**
 * @author hme
 * @create 2024-02-27-17:22
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    private boolean debug = true;
    private static String dynamicPrefix = "dymic";
    private Map<Object, Object> dynamicTargetDataSources;
    private Object dynamicDefaultTargetDataSource;

    /**
     * 检查当前数据源是否存在，如果存在测试是否可用。不存在或者不可用创建新的数据源
     *
     * @param dbConfig
     * @throws Exception
     */
    public void createDataSourceWithCheck(db_connect_list dbConfig) throws Exception {
        String datasourceName = dynamicPrefix + dbConfig.getDb_connect_id().toString();
        log.info("checking the datasource：" + datasourceName);
        Map<Object, Object> dynamicTargetDataSources2 = this.dynamicTargetDataSources;
        if (dynamicTargetDataSources2.containsKey(datasourceName)) {
            log.info("datasource " + datasourceName + "has been created，prepare to check if the datasource works...");
            DruidDataSource druidDataSource = (DruidDataSource) dynamicTargetDataSources2.get(datasourceName);
            boolean rightFlag = true;
            Connection connection = null;
            try {
                log.info(datasourceName + "the overview of datasource ->free connections：" + druidDataSource.getPoolingCount());
                long activeCount = druidDataSource.getActiveCount();
                log.info(datasourceName + "the overview of datasource->the amount of active connections ：" + activeCount);
                if (activeCount > 0) {
                    log.info(datasourceName + "the overview of datasource->active stack info：" + druidDataSource.getActiveConnectionStackTrace());
                }
                log.info("prepare to open connection...");
                connection = druidDataSource.getConnection();
                log.info("datasource " + datasourceName + " works");
            } catch (Exception e) {
                log.error(e.getMessage(), e); //把异常信息打印到日志文件
                rightFlag = false;
                log.info("cache datasource " + datasourceName + "has expired，prepare to delete...");
                if (delDatasources(datasourceName)) {
                    log.info("datasource delete succeed");
                } else {
                    log.info("datasource delete failed");
                }
            } finally {
                if (null != connection) {
                    connection.close();
                }
            }
            if (rightFlag) {
                log.info("don't need to recreate the datasource");
                return;
            } else {
                log.info("prepare to recreate datasource...");
                createDataSource(dbConfig, datasourceName);
                log.info("datasource recreated succeed ");
            }
        } else {
            createDataSource(dbConfig, datasourceName);
        }
    }

    /**
     * 真正的创建数据源的方法
     *
     * @param dbConfig
     * @return
     */
    public boolean createDataSource(db_connect_list dbConfig, String datasourceName) {
        try {
            String dbUrl = "";
            String validationQuery = "select 1";
            if (dbConfig.getDatabaseType().getName().equals("Oracle")) {
                dbUrl = dbConfig.getDatabaseType().getDatabase_connect_template().replace("db_connect_ip", dbConfig.getDb_connect_ip())
                        .replace("db_connect_port", dbConfig.getDb_connect_port().toString())
                        .replace("db_connect_dbname", dbConfig.getDb_connect_instance());
                validationQuery = "select 1 from dual";
            } else {
                dbUrl = dbConfig.getDatabaseType().getDatabase_connect_template().replace("db_connect_ip", dbConfig.getDb_connect_ip())
                        .replace("db_connect_port", dbConfig.getDb_connect_port().toString())
                        .replace("db_connect_dbname", dbConfig.getDb_connect_dbname());
            }
            try { // 排除连接不上的错误
                Class.forName(dbConfig.getDatabaseType().getDriver_class_name());

                DriverManager.getConnection(dbUrl, dbConfig.getDb_connect_username(), dbConfig.getDb_connect_password());// 相当于连接数据库
            } catch (Exception e) {
                log.info("datasource connect failed ,url:{},username:{},password:{},错误原因：{}",
                        dbUrl, dbConfig.getDb_connect_username(), dbConfig.getDb_connect_password(), e.getMessage());
                return false;
            }
            @SuppressWarnings("resource")
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setName(dbConfig.getDb_connect_dbname());
            druidDataSource.setDriverClassName(dbConfig.getDatabaseType().getDriver_class_name());
            druidDataSource.setUrl(dbUrl);
            druidDataSource.setUsername(dbConfig.getDb_connect_username());
            druidDataSource.setPassword(dbConfig.getDb_connect_password());
            druidDataSource.setInitialSize(1); //初始化时建立物理连接的个数。初始化发生在显示调用init方法，或者第一次getConnection时
            druidDataSource.setMaxActive(20); //最大连接池数量
            druidDataSource.setMaxWait(60000); //获取连接时最大等待时间，单位毫秒。当链接数已经达到了最大链接数的时候，应用如果还要获取链接就会出现等待的现象，等待链接释放并回到链接池，如果等待的时间过长就应该踢掉这个等待，不然应用很可能出现雪崩现象
            druidDataSource.setMinIdle(5); //最小连接池数量

            druidDataSource.setValidationQuery(validationQuery); //用来检测连接是否有效的sql，要求是一个查询语句。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。
            druidDataSource.setTestOnBorrow(true); //申请连接时执行validationQuery检测连接是否有效，这里建议配置为TRUE，防止取到的连接不可用
            druidDataSource.setTestWhileIdle(true);//建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。
            druidDataSource.setFilters("stat");//属性类型是字符串，通过别名的方式配置扩展插件，常用的插件有：监控统计用的filter:stat日志用的filter:log4j防御sql注入的filter:wall
            druidDataSource.setTimeBetweenEvictionRunsMillis(60000); //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
            druidDataSource.setMinEvictableIdleTimeMillis(180000); //配置一个连接在池中最小生存的时间，单位是毫秒，这里配置为3分钟180000
            druidDataSource.setKeepAlive(true); //打开druid.keepAlive之后，当连接池空闲时，池中的minIdle数量以内的连接，空闲时间超过minEvictableIdleTimeMillis，则会执行keepAlive操作，即执行druid.validationQuery指定的查询SQL，一般为select * from dual，只要minEvictableIdleTimeMillis设置的小于防火墙切断连接时间，就可以保证当连接空闲时自动做保活检测，不会被防火墙切断
            druidDataSource.setRemoveAbandoned(true); //是否移除泄露的连接/超过时间限制是否回收。
            druidDataSource.setRemoveAbandonedTimeout(3600); //泄露连接的定义时间(要超过最大事务的处理时间)；单位为秒。这里配置为1小时
            druidDataSource.setLogAbandoned(true);
            druidDataSource.init();
            this.dynamicTargetDataSources.put(datasourceName, druidDataSource);
            setTargetDataSources(this.dynamicTargetDataSources);// 将map赋值给父类的TargetDataSources
            super.afterPropertiesSet();// 将TargetDataSources中的连接信息放入resolvedDataSources管理
            log.info(dbConfig.getDb_connect_dbname() + "datasource init succeed");
            return true;
        } catch (Exception e) {
            log.error(e + "");
            return false;
        }

    }

    /**
     * 删除数据源
     */
    public boolean delDatasources(String datasourceName) {
        Map<Object, Object> dynamicTargetDataSources2 = this.dynamicTargetDataSources;
        if (dynamicTargetDataSources2.containsKey(datasourceName)) {
            Set<DruidDataSource> druidDataSourceInstances = DruidDataSourceStatManager.getDruidDataSourceInstances();
            for (DruidDataSource l : druidDataSourceInstances) {
                if (datasourceName.equals(l.getName())) {
                    dynamicTargetDataSources2.remove(datasourceName);
                    DruidDataSourceStatManager.removeDataSource(l);
                    setTargetDataSources(dynamicTargetDataSources2);// 将map赋值给父类的TargetDataSources
                    super.afterPropertiesSet();// 将TargetDataSources中的连接信息放入resolvedDataSources管理
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }


    /**
     * 测试数据源连接是否有效
     *
     * @param dbConfig
     * @return
     */
    public boolean testDatasource(db_connect_list dbConfig) {
        try {
            Class.forName(dbConfig.getDatabaseType().getDriver_class_name());
            String dbUrl = dbConfig.getDatabaseType().getDatabase_connect_template().replace("db_connect_ip", dbConfig.getDb_connect_ip())
                    .replace("db_connect_port", dbConfig.getDb_connect_port().toString())
                    .replace("db_connect_dbname", dbConfig.getDb_connect_dbname());
            DriverManager.getConnection(dbUrl, dbConfig.getDb_connect_username(), dbConfig.getDb_connect_password());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        this.dynamicDefaultTargetDataSource = defaultTargetDataSource;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String datasource = DBContextHolder.getDataSource();
        if (!StringUtils.isEmpty(datasource)) {
            Map<Object, Object> dynamicTargetDataSources2 = this.dynamicTargetDataSources;
            if (dynamicTargetDataSources2.containsKey(datasource)) {
                log.info("---current datasource：" + datasource + "---");
            } else {
                log.info("datasource can not found：");
                return null;
            }
        } else {
            log.info("---current datasource：default datasource---");
        }
        return datasource;
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        this.dynamicTargetDataSources = targetDataSources;
    }

    public Map<Object, Object> getDynamicTargetDataSources() {
        return dynamicTargetDataSources;
    }

    public void setDynamicTargetDataSources(Map<Object, Object> dynamicTargetDataSources) {
        this.dynamicTargetDataSources = dynamicTargetDataSources;
    }

    public Object getDynamicDefaultTargetDataSource() {
        return dynamicDefaultTargetDataSource;
    }

    public void setDynamicDefaultTargetDataSource(Object dynamicDefaultTargetDataSource) {
        this.dynamicDefaultTargetDataSource = dynamicDefaultTargetDataSource;
    }
}
