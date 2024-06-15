-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT DISTINCT name
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person)
          -[e IS friends WHERE e.meeting_date > DATE '2000-09-15']-{2}
            (b IS person)
  WHERE a.name = 'John' AND a.name <> b.name
  COLUMNS (b.name)
);