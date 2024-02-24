package com.github.segu23.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RelationalEntity {

    private String entityName;

    private List<EntityColumn> columns = new ArrayList<>();
}
