-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person) <-[e1 IS friends]- (b IS person),
        (b) <-[e2 IS friends]- (c IS person),
        (c) <-[e3 is friends]- (a IS person)
  WHERE a.name= 'Mary'
  COLUMNS (a.name AS person_a, b.name AS person_b, c.name AS person_c)
);