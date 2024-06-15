-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
ALTER TABLE jsontab1
ADD jschd JSON CONSTRAINT jschdsv
                   CHECK (jschd IS JSON VALIDATE USING '{"type":"string"}');

Table altered.