package com.github.segu23.projectgenerator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableRelation {

    private String tableName;
    private String externalTableColumn;
}
