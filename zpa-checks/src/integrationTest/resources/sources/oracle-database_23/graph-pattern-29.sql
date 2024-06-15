-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (x IS person|university)
  COLUMNS (x.name, x.dob)
)
ORDER BY name;