[![Build Status](https://travis-ci.org/arnaudroger/SimpleFlatMapper.svg?branch=master)](https://travis-ci.org/arnaudroger/SimpleFlatMapper)
[![Coverage Status](https://img.shields.io/coveralls/arnaudroger/SimpleFlatMapper.svg)](https://coveralls.io/r/arnaudroger/SimpleFlatMapper)

SimpleFlatMapper
========
Fast and Easy mapping from database to POJO. 
A super lightweight no configuration ORM alternative to iBatis or Hibernate.

- Compatible with Java 6, 7 and 8. 
- [Lambda Ready](#jdbcmapper).
- easy to integrate with [Spring JdbcTemplate](#jdbctemplate). 
- [Osgi](#osgisupport) ready.
- [QueryDSL Jdbc support](#querydsl-jdbc)
- [CsvMapper](#csvmapper)

Feedbacks are welcome specially from real use case, don't hesitate to raise an issue or send me an email.

Design
========
- no configuration
- low foot print
- use plain jdbc
- no external library needed
- respect final fields
- support asm generation for max performance

What it does not do
-------
- no query generation
- no insert/update
- no caching
- no object lifecycle

Why?
-------

[Mapping Landscape](https://github.com/arnaudroger/SimpleFlatMapper/wiki/Mapping-Landscape)

### Performance

Mybatis and Hibernate have very expensive injection mechanism. On the hsqldb in memory the markup for a medium size query is [400%](#in-mem-hsqldb) for Hibernate and 500% for MyBatis. 

BeanPropertyRowMapper is very slow.

Sfm is as fast as it can using asm generation. Even if you don't use asm it is still a lot faster. 


### API intrusiveness

Ibatis provide the same kind of functionality put it forces you to use it's query mechanism and mask the jdbc api. 
Sfm just focus on the mapping from a [ResultSet](#jdbcmapper). You can manage the query the way you want. You can use [JdbcTemplate](#jdbctemplate), even use it in an Hibernate session via the doWork method.

Samples
========

JdbcMapper
---------

```java

public class MyDao {
    private final JdbcMapper<MyObject> mapper = 
    	JdbcMapperFactory.newInstance().newMapper(MyObject.class);
    public void printAllLambda(Writer writer, Connection conn) throws SQLException {
        try (PreparedStatement ps = 
        		conn.prepareStatement("select id, email, my_property from MyTable")) {
	        try (ResultSet rs = ps.executeQuery()){
	            mapper.forEach(rs, (o) -> writer.append(o.toString()).append("\n"));
	        }
        }
    }
    public void printAll(Writer writer, Connection conn) throws SQLException {
        try (PreparedStatement ps = 
        		conn.prepareStatement("select id, email, my_property from MyTable")) {
	        try (ResultSet rs = ps.executeQuery()){
	            mapper.forEach(rs, new RowHandler<MyObject>{
	            	public void handle(MyObject o) throws IOException { 
	            		writer.append(o.toString()).append("\n")); 
	            	}  
	            });
	        }
        }
    }
}
```

JdbcTemplate
-----

See [JdbcTemplateMapperFactoryTest](/src/test/java/org/sfm/jdbc/spring/JdbcTemplateMapperFactoryTest.java) for more examples.

```java
class MyDao {
	private final JdbcTemplateMapper<DbObject> mapper = 
		JdbcTemplateMapperFactory.newInstance().newMapper(DbObject.class);
		
	public void doSomething() {
		List<DbObject> results = template.query(DbHelper.TEST_DB_OBJECT_QUERY, mapper);
	}
	
	public void doSomethingElse() {
		 template
		 	.query(TEST_DB_OBJECT_QUERY, 
		 		mapper.newResultSetExtractor((o) -> System.out.println(o.toString())));
	}
}
```

OsgiSupport
------
The Osgi support just expose a service that will deal with the classloading wizardry needed to generate bytecode.

```java
class MyService {

	@Reference
	JdbcMapperService jdbcMapperService;
	
	volatile JdbcMapper mapper;
	
	@Activate
	public void activate() {
		mapper = jdbcMapperService.newFactory().newMapper(DbObject.class);
	}
}
```

QueryDSL Jdbc
------

```java
SQLQuery sqlquery = new SQLQueryImpl(conn, new HSQLDBTemplates());
try {
	return sqlquery
		.from(qTestDbObject)
		.where(qTestDbObject.id.eq(1l))
		.list(new QueryDslMappingProjection<DbObject>(DbObject.class, 
				qTestDbObject.id,
				qTestDbObject.name, 
				qTestDbObject.email, 
				qTestDbObject.creationTime, 
				qTestDbObject.typeName, 
				qTestDbObject.typeOrdinal ));
} finally {
	conn.close();
}
```

CsvMapper
---------

Uses header to match to the property.

```java
public class MyParser {
    private final CsvMapper<MyObject> mapper = 
    	CsvMapperFactory.newInstance().newMapper(MyObject.class);
    public void printAll(Writer writer, Reader reader) throws IOException {
        mapper.forEach(reader, (o) -> writer.append(o.toString()).append("\n"));
    }
}
```



Property Mapping
========

the mapper will assume a column name from the database will be matching the property name ignoring the case and underscores.

ie:
```
- my_property => myProperty
- myproperty => myProperty
```


Value Injection
------

The JdbcMapper supports
- constructor injection - needs asm to get the parameters name -
- setter injection
- field injection
It looks for injection on that order and if asm is present will generate optimised asm version.


```sql
create table MyTable {
	id bigint,
	email varchar(256),
	my_property int
}
```

```java
public class MyObject {
	private final long id;
	private final String email;
	private final int myProperty;
	
	public MyObject(long id, String email,  int myProperty) {
		this.id = id;
		this.email = email;
		this.myProperty = myProperty;
	}

	public long getId() { return id; }
	public String getEmail() { return email; }
	public int getProperty() { return myProperty; }
	
	public String toString() { ... }
}
```

Inner object mapping
-------

It also supports complex object injection via constructor, field or setter.

```java
public class OuterObject {
	String id;
	MyObject subObject;
}
```

```sql
select id, sub_object_id, sub_object_email, sub_object_my_property
```


List Mapping
-------

And list mapping in an object or at first level.

```java
public class ListObject {
	String id;
	List<MyObject> subObjects;
}
```

```sql
select id, 
	sub_objects_0_id, sub_objects_0_email, sub_objects_0_my_property, 
	sub_objects_1_id, 
	sub_objects_3_id
```

Performance
========

New Results, with better consistency. Use a slightly modified BoneCP and PreparedStatement cache. 
Use JMH to with sample mode. I still need to validate the mysql and hsqldb result via a profiler.
I will try to see how to get the static mapper closer to Roma. Because Roma assume that there is a 1 to 1 match between column and property it does not check for a column change between query.

See [orm-benchmarks](https://github.com/arnaudroger/orm-benchmark) for more details.


Mock Connection
-------

|Benchmark|1|10|100|1000|
|---------|---:|---:|---:|---:|
|JdbcMapperStatic|46%|106%|141%|193%|
|JdbcMapperDynamic|103%|126%|145%|224%|
|JdbcMapperDynamicNoAsm|260%|632%|939%|1101%|
|Roma|15%|81%|127%|131%|
|Sql2o|4565%|3272%|2970%|2675%|
|Hibernate|29984%|22163%|17873%|17920%|
|MyBatis|18541%|24605%|28910%|30041%|
|RowMapper|16396%|62859%|94365%|100019%|

In mem HsqlDb
-------

|Benchmark|1|10|100|1000|
|---------|---:|---:|---:|---:|
|JdbcMapperStatic|39%|108%|140%|194%|
|JdbcMapperDynamic|102%|125%|144%|226%|
|JdbcMapperDynamicNoAsm|252%|618%|936%|1104%|
|Roma|13%|79%|124%|132%|
|Sql2o|4595%|3250%|2998%|2673%|
|Hibernate|29624%|22027%|17766%|18341%|
|MyBatis|18206%|24424%|28842%|30290%|
|RowMapper|16402%|63278%|93224%|100358%|

Local Mysql
-------

|Benchmark|1|10|100|1000|
|---------|---:|---:|---:|---:|
|JdbcMapperStatic|43%|101%|140%|192%|
|JdbcMapperDynamic|104%|123%|142%|222%|
|JdbcMapperDynamicNoAsm|262%|617%|939%|1115%|
|Roma|14%|77%|123%|132%|
|Sql2o|4699%|3215%|2990%|2671%|
|Hibernate|29593%|21783%|17723%|18228%|
|MyBatis|18417%|24373%|28708%|29829%|
|RowMapper|16140%|60963%|94062%|99111%|

Csv Mapping 
-------

Beta results.
Reads a 1000 rows in memory csv. 

```
Benchmark                                         Mode  Samples     Score  Score error  Units
o.s.b.c.CsvBenchmark.testReadCsvJackson1000      thrpt      200  1992.767        6.634  ops/s
o.s.b.c.CsvBenchmark.testReadCsvSfm1000Reader    thrpt      200  3096.793        8.749  ops/s
```

Maven dependency
======

```xml
		<dependency>
			<groupId>com.github.arnaudroger</groupId>
			<artifactId>simpleFlatMapper</artifactId>
			<version>0.9.6</version>
		</dependency>
```

