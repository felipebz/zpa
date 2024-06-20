-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (v)
  COLUMNS ( v.* )
)
ORDER BY 1, 2, 3, 4, 5;