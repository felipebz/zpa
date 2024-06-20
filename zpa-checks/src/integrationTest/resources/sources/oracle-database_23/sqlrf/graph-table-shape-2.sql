-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (p1 IS person) -[e IS friends]-> (p2 IS person)
  COLUMNS ( p1.*, p2.name AS p2_name, e.* )
)
ORDER BY 1, 2, 3, 4, 5;