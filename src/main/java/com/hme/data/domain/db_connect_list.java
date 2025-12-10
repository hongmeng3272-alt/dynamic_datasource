/***********************************************************************
 * Module:  db_connect_list.java
 * Author:  hme
 * Purpose: Defines the Class db_connect_list
 ***********************************************************************/
package com.hme.data.domain;
import lombok.Data;

import java.util.*;

@Data
public class db_connect_list extends base_document_info {
   public Integer db_connect_id;

   public Integer database_type_id;

   public Integer data_area_id;

   public String db_connect_name;

   public String db_connect_ip;

   public Integer db_connect_port;

   public String db_connect_username;

   public String db_connect_password;

   public String db_connect_dbname;

   public String db_connect_instance;
   //关联数据库类型
   private database_type databaseType;

}