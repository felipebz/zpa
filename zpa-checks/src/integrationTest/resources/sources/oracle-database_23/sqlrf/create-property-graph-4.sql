-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “myGraph” 
     VERTEX TABLES (
       VT1,
       VT2 KEY(PK2),
       VT3 KEY(PK31, PK32),
       VT2 AS ALTVT2 KEY(PK4)
     )
     EDGE TABLES (
       E1 SOURCE VT1  
          DESTINATION VT2,
       E2 SOURCE      KEY(FK1) REFERENCES VT1 (PK1) 
          DESTINATION KEY(FK2) REFERENCES VT2 (PK2),
       E3 SOURCE      KEY(FK1) REFERENCES VT1 (PK1) 
          DESTINATION VT2,
       E4 SOURCE VT1  
          DESTINATION KEY(FK5) REFERENCES VT2(RK5))
;