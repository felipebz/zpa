-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT Gt.name
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person WHERE a.name = 'John')
          -[e IS friends WHERE e.meeting_date > DATE '2000-09-15']-
            (b IS person)
  COLUMNS (b.name)
) GT;