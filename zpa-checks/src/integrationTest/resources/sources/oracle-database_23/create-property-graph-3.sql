-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “myGraph” VERTEX TABLES (my_table_1, other_schema.my_table_1 AS my_table2);