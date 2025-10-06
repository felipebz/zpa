-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-reference.html
SELECT COUNT(*)
FROM GRAPH_TABLE (
  students_graph AS OF TIMESTAMP (SYSTIMESTAMP - INTERVAL '2' MINUTE)
  MATCH (u IS university)
  COLUMNS (u.*)
);