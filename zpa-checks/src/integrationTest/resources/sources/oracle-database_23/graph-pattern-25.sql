-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (p1 IS person) -[e1 IS friends]- (p2 IS person)
      , (p1) -[IS student_of]-> (u1 IS university)
      , (p2) -[IS student_of]-> (u2 IS university)
  WHERE p1.name = 'Mary'
  COLUMNS (p1.name, p2.name AS friend, e1.meeting_date, u1.name AS univ_1, u2.name AS univ_2)
);