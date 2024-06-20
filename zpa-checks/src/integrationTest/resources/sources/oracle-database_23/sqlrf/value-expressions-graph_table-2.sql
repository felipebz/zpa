-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (n IS person)
  COLUMNS ( n.name, n.height )
)
ORDER BY height;