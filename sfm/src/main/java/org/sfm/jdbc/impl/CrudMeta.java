package org.sfm.jdbc.impl;

import org.sfm.jdbc.JdbcMapperFactory;
import org.sfm.jdbc.SqlTypeColumnProperty;
import org.sfm.map.column.KeyProperty;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CrudMeta<T, K> {

    private final DatabaseMeta databaseMeta;
    private final String table;
    private final ColumnMeta[] columnMetas;

    public CrudMeta(DatabaseMeta databaseMeta, String table, ColumnMeta[] columnMetas) {
        this.databaseMeta = databaseMeta;
        this.table = table;
        this.columnMetas = columnMetas;
    }

    public DatabaseMeta getDatabaseMeta() {
        return databaseMeta;
    }

    public String getTable() {
        return table;
    }

    public ColumnMeta[] getColumnMetas() {
        return columnMetas;
    }

    public boolean hasGeneratedKeys() {
        for(ColumnMeta cm : columnMetas) {
            if (cm.isKey() && cm.isGenerated()) {
                return true;
            }
        }
        return false;
    }

    public static <T, K> CrudMeta<T, K> of(Connection connection, String table) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE 1 = 2");
            try {


                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

                DatabaseMetaData metaData = connection.getMetaData();
                DatabaseMeta databaseMeta = new DatabaseMeta(metaData.getDatabaseProductName(), metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
                ColumnMeta[] columnMetas = new ColumnMeta[resultSetMetaData.getColumnCount()];
                List<String> primaryKeys = getPrimaryKeys(connection, resultSetMetaData);

                for(int i = 0; i < columnMetas.length; i++) {
                    String columnName = resultSetMetaData.getColumnName(i + 1);
                    columnMetas[i] = new ColumnMeta(
                            columnName,
                            resultSetMetaData.getColumnType(i + 1),
                            primaryKeys.contains(columnName),
                            resultSetMetaData.isAutoIncrement(i + 1));
                }

                return new CrudMeta<T, K>(databaseMeta, table, columnMetas);


            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }

    }

    private static List<String> getPrimaryKeys(Connection connection, ResultSetMetaData resultSetMetaData) throws SQLException {
        List<String> primaryKeys = new ArrayList<String>();
        ResultSet set = connection.getMetaData().getPrimaryKeys(resultSetMetaData.getCatalogName(1), resultSetMetaData.getSchemaName(1), resultSetMetaData.getTableName(1));
        try {
            while (set.next()) {
                primaryKeys.add(set.getString("COLUMN_NAME"));
            }
        } finally {
            set.close();
        }
        return primaryKeys;
    }

    public void addColumnProperties(JdbcMapperFactory mapperFactory) {
        for(ColumnMeta columnMeta : columnMetas) {
            mapperFactory.addColumnProperty(columnMeta.getColumn(), SqlTypeColumnProperty.of(columnMeta.getSqlType()));
            if (columnMeta.isGenerated()) {
                mapperFactory.addColumnProperty(columnMeta.getColumn(), new KeyProperty());
            }
        }
    }
}
