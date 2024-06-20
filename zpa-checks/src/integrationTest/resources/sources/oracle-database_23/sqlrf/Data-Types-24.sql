-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
SELECT * FROM example e1
WHERE c1 >= ALL (SELECT c2 FROM example e2 WHERE e2.id > e1.id);  