-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT CAST(p2_id AS VARCHAR2(200)) AS p2_id
FROM GRAPH_TABLE ( students_graph
  MATCH (p1 IS person) -[e1 IS friends]- (p2 IS person)
  WHERE p1.name = 'Mary'
  COLUMNS (vertex_id(p2) AS p2_id)
)
ORDER BY p2_id;