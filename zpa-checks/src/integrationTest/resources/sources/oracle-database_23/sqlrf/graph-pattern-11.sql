-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (n IS person) (-[IS friends]-){2}(m IS person)
  WHERE n.name = 'Mary' AND m.name <> n.name
  COLUMNS (m.name AS fof)
);