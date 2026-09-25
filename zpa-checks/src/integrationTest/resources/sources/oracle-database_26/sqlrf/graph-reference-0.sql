-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph-reference.html
SELECT COUNT(*)
FROM GRAPH_TABLE ( scott.students_graph
  MATCH (a IS person)
  COLUMNS (a.name)
);