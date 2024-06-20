-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “mygraph” 
  VERTEX TABLES (VT1, VT2 KEY(PK2)),
  EDGE TABLES ( 
    ET1 SOURCE VT1 DESTINATION VT2, 
    ET2 SOURCE KEY(FK2) REFERENCES VT2 (PK2) DESTINATION VT1) 
 OPTIONS(ENFORCED MODE);