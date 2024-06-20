-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (n IS person WHERE n.name = 'Mary')
          -[e IS friends WHERE e.meeting_date > DATE '2001-01-01']-
            () -[IS friends]- (m IS person)
  WHERE m.name <> n.name
  COLUMNS (m.name, e.meeting_date)
);