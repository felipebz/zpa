-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT COUNT(*)
FROM GRAPH_TABLE ( students_graph
  MATCH ->
  COLUMNS (1 AS dummy)
);