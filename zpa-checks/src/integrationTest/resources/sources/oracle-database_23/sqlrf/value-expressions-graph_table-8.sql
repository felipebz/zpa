-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT DISTINCT json_value(e_id, '$.ELEM_TABLE') AS elem_table
FROM GRAPH_TABLE ( students_graph
  MATCH -[e]-
  COLUMNS (edge_id(e) AS e_id)
)
ORDER BY elem_table;