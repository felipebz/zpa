-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph_table-operator.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person) -[e IS friends]- (b IS person)
  WHERE a.name = 'John'
  COLUMNS (b.name)
);