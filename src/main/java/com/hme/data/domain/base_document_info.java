/***********************************************************************
 * Module:  base_document_info.java
 * Author:  hme
 * Purpose: Defines the Class base_document_info
 ***********************************************************************/

package com.hme.data.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class base_document_info implements Serializable {
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd hh:mm:ss")
    public Date deactivate_time;
    public String last_update_user_id;
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd hh:mm:ss")
    public Date last_update_time;
    public String last_update_user_name;
}

