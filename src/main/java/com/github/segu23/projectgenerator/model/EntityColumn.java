package com.github.segu23.projectgenerator.model;

import lombok.Data;

@Data
public class EntityColumn {

    private String columnName;
    private String columnType;
    private Integer columnSize;
    private Boolean nullable;
    private Boolean primaryKey;
    private TableRelation relation;
}
