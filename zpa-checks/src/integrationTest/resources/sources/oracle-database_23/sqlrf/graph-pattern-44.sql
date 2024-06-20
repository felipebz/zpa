-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT Gt.name
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person) -[e IS friends]- (b IS person)
  WHERE a.name = 'John' AND e.meeting_date > DATE '2000-09-15'
  COLUMNS (b.name)
) GT;