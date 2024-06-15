-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT GT.p2_id.KEY_VALUE
FROM GRAPH_TABLE ( students_graph
  MATCH (p1 IS person) -[e1 IS friends]- (p2 IS person)
  WHERE p1.name = 'Mary'
  COLUMNS (vertex_id(p2) AS p2_id)
) GT
ORDER BY key_value;