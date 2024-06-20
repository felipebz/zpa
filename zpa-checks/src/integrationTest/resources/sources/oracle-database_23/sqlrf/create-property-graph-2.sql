-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “myGraph” 
     VERTEX TABLES (
       VT1,
       VT2 KEY(PK2),
       VT3 KEY(PK31, PK32),
       VT2 AS ALTVT2 KEY(PK4)
     );