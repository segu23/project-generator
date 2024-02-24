package com.github.segu23;

import com.github.segu23.model.EntityColumn;
import com.github.segu23.model.RelationalEntity;
import com.github.segu23.model.TableRelation;
import com.google.gson.Gson;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;

import java.io.*;
import java.util.*;

public class Main {

    private final static Gson GSON_INSTANCE = new Gson();

    public static void main(String[] args) throws Exception {
        Map<String, String> programArguments = parseCommandLineArgs(args);

        if (!programArguments.containsKey("file")) {
            throw new Exception("Necesitas especificar la ruta origen de tu archivo SQL");
        }
        if (!programArguments.containsKey("output")) {
            throw new Exception("Necesitas especificar la ruta destino de tu archivo JSON");
        }

        String path = programArguments.get("file");

        if(!new File(path).exists() || !path.endsWith(".sql")){
            throw new Exception("La ruta origen ingresada no corresponde a un archivo válido");
        }
        List<String> createTableStatements = extractCreateTableStatements(path);
        Map<String, RelationalEntity> relationalEntityMap = extractEntities(createTableStatements);

        try (FileWriter writer = new FileWriter(programArguments.get("output"))) {
            GSON_INSTANCE.toJson(relationalEntityMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, RelationalEntity> extractEntities(List<String> createTableStatements) {
        Map<String, RelationalEntity> relationalEntityMap = new HashMap<>();

        for (String createTableStatement : createTableStatements) {
            try {
                CreateTable createTable = parseCreateTableStatement(createTableStatement);
                RelationalEntity relationalEntity = new RelationalEntity();

                String tableName = createTable.getTable().getName().replaceAll("`", "");
                relationalEntity.setEntityName(tableName);

                List<EntityColumn> entityColumns = getEntityColumns(createTable);
                relationalEntity.setColumns(entityColumns);

                relationalEntityMap.put(tableName, relationalEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return relationalEntityMap;
    }

    private static List<EntityColumn> getEntityColumns(CreateTable createTable) {
        Map<String, TableRelation> tableRelations = extractTableRelations(createTable);
        List<String> tablePrimaryKeys = extractTablePrimaryKeys(createTable);
        List<EntityColumn> entityColumns = createTable.getColumnDefinitions().stream()
                .map(columnDefinition -> {
                    EntityColumn entityColumn = new EntityColumn();
                    entityColumn.setColumnName(columnDefinition.getColumnName().replaceAll("`", ""));
                    entityColumn.setPrimaryKey(tablePrimaryKeys.contains(entityColumn.getColumnName()));
                    if (columnDefinition.getColDataType().getArgumentsStringList() != null)
                        entityColumn.setColumnSize(Integer.valueOf(columnDefinition.getColDataType().getArgumentsStringList().get(0)));
                    entityColumn.setColumnType(columnDefinition.getColDataType().getDataType());
                    entityColumn.setNullable(!new HashSet<>(columnDefinition.getColumnSpecs()).containsAll(List.of("NOT", "NULL")));
                    TableRelation tableRelation = tableRelations.get(entityColumn.getColumnName());
                    if (tableRelations.containsKey(entityColumn.getColumnName()))
                        entityColumn.setRelation(tableRelation);
                    return entityColumn;
                })
                .toList();
        return entityColumns;
    }

    private static List<String> extractTablePrimaryKeys(CreateTable createTable) {
        return createTable.getIndexes().stream()
                .filter(index -> index.getType().equals("PRIMARY KEY"))
                .flatMap(index -> index.getColumnsNames().stream())
                .map(columnName -> columnName.replaceAll("`", ""))
                .toList();
    }

    private static Map<String, TableRelation> extractTableRelations(CreateTable createTable) {
        Map<String, TableRelation> tableRelations = new HashMap<>();

        createTable.getIndexes().forEach(index -> {
            if (index instanceof ForeignKeyIndex foreignKeyIndex) {
                for (int i = 0; i < index.getColumnsNames().size(); i++) {
                    TableRelation tableRelation = new TableRelation(
                            foreignKeyIndex.getTable().getName().replaceAll("`", ""),
                            foreignKeyIndex.getReferencedColumnNames().get(i).replaceAll("`", "")
                    );
                    tableRelations.put(index.getColumnsNames().get(i).replaceAll("`", ""), tableRelation);
                }
            }
        });
        return tableRelations;
    }

    private static List<String> extractCreateTableStatements(String fileName) {
        List<String> createTableStatements = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            StringBuilder sb = new StringBuilder();
            boolean insideCreateTable = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("CREATE TABLE")) {
                    insideCreateTable = true;
                    sb = new StringBuilder();
                }
                if (insideCreateTable) {
                    sb.append(line).append("\n");
                    if (line.contains(";")) {
                        createTableStatements.add(sb.toString());
                        insideCreateTable = false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createTableStatements;
    }

    private static CreateTable parseCreateTableStatement(String sql) throws Exception {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof CreateTable createTable) {
                return createTable;
            } else {
                System.err.println("No es una sentencia CREATE TABLE.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception();
    }

    public static Map<String, String> parseCommandLineArgs(String[] args) {
        Map<String, String> params = new HashMap<>();

        for (String arg : args) {
            // Verificar si el argumento tiene el formato -key=value
            if (arg.startsWith("-") && arg.contains("=")) {
                // Dividir el argumento en la clave y el valor
                String[] parts = arg.substring(1).split("=");
                String key = parts[0];
                String value = parts[1];
                params.put(key, value);
            }
        }

        return params;
    }

}