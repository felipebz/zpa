-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (n IS person) -[e IS student_of|friends]-> (m IS university|"PERSON")
  WHERE n.name = 'Mary'
  COLUMNS (e.subject, e.meeting_date, m.name)
)
ORDER BY subject, meeting_date, name;