-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
CREATE TABLE jsontab1(
    id NUMBER(4),
    j  JSON CONSTRAINT jt1isj CHECK (j IS JSON VALIDATE USING     
     '{
       "type":"object", 
       "minProperties": 2
      }')
    );