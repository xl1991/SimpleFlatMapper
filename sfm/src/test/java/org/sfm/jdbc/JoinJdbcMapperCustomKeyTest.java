package org.sfm.jdbc;

import org.junit.Test;
import org.sfm.map.column.FieldMapperColumnDefinition;
import org.sfm.reflect.meta.PropertyMeta;
import org.sfm.utils.ListCollectorHandler;
import org.sfm.utils.Predicate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.sfm.jdbc.JoinJdbcMapperOnTupleTest.*;

public class JoinJdbcMapperCustomKeyTest {

    public static class PersonKey {
        public String tag;
        public int n;
    }

    public static class Person {
        public PersonKey key;
        public String name;
        public List<String> phones;
    }

    @Test
    public void test() throws SQLException {
        JdbcMapper<Person> mapper = JdbcMapperFactory.newInstance()
                .addColumnDefinition(
                        new Predicate<JdbcColumnKey>() {
                            @Override
                            public boolean test(JdbcColumnKey jdbcColumnKey) {
                                return jdbcColumnKey.getIndex() <= 2;
                            }
                        },
                        FieldMapperColumnDefinition.<JdbcColumnKey>key(
                                new Predicate<PropertyMeta<?, ?>>() {
                                    @Override
                                    public boolean test(PropertyMeta<?, ?> propertyMeta) {
                                        return propertyMeta.getPath().startsWith("key.");
                                    }
                                }
                        ))
                .newBuilder(Person.class)
                .addMapping("key_tag")
                .addMapping("key_n")
                .addMapping("name")
                .addMapping("phones_value")
                .mapper();

        ResultSet rs =
                setUpResultSetMock(new String[]{"key_tag", "key_n", "name", "phone_value"},
                        new Object[][]{
                                {"t1", 1, "t11", "p111"},
                                {"t1", 1, "t11", "p112"},
                                {"t1", 2, "t12", "p121"}
                        });

        final List<Person> list = mapper.forEach(rs, new ListCollectorHandler<Person>()).getList();

        assertEquals(2, list.size());
        assertEquals("t1", list.get(0).key.tag);
        assertEquals(1, list.get(0).key.n);
        assertEquals("t11", list.get(0).name);
        assertEquals(Arrays.asList("p111", "p112"), list.get(0).phones);

        assertEquals("t1", list.get(1).key.tag);
        assertEquals(2, list.get(1).key.n);
        assertEquals("t12", list.get(1).name);
        assertEquals(Arrays.asList("p121"), list.get(1).phones);

    }

}
