package org.sfm.csv.impl;

import org.sfm.csv.CsvColumnDefinition;
import org.sfm.csv.CsvColumnKey;
import org.sfm.map.column.ColumnProperty;
import org.sfm.map.mapper.ColumnDefinitionProvider;
import org.sfm.utils.BiConsumer;
import org.sfm.utils.Predicate;

public class IdentityCsvColumnDefinitionProvider implements ColumnDefinitionProvider<CsvColumnDefinition, CsvColumnKey> {
    @Override
    public CsvColumnDefinition getColumnDefinition(CsvColumnKey key) {
        return CsvColumnDefinition.identity();
}

    @Override
    public <CP extends ColumnProperty, BC extends BiConsumer<Predicate<? super CsvColumnKey>, CP>> BC forEach(Class<CP> propertyType, BC consumer) {
        return consumer;
    }
}
