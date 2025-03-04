-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-reference.html
SELECT COUNT(*)
FROM GRAPH_TABLE (
  students_graph
  MATCH (u IS university)
  COLUMNS (u.*)
);