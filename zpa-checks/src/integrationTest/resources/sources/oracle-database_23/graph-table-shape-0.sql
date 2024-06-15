-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (n IS person|person_ht)
  COLUMNS (n.name, n.height * 3.281 AS height_in_feet)
)
ORDER BY name;