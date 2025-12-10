/***********************************************************************
 * Module:  database_type.java
 * Author:  hme
 * Purpose: Defines the Class database_type
 ***********************************************************************/
package com.hme.data.domain;

import lombok.Data;

import java.util.*;
@Data
public class database_type {
   public Integer database_type_id;
   public String code;
   public String name;
   public Integer db_relationship_type_id;
   public String database_connect_template;
   public String driver_class_name;
}