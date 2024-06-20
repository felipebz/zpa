-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT DISTINCT name, height
FROM GRAPH_TABLE ( students_graph
  MATCH (a IS person|person_ht)
           (-[e IS friends]- (x IS person_ht) WHERE x.height > a.height) {,3}
             (b IS person|person_ht)
  WHERE a.name = 'Mary'
  COLUMNS (b.name, b.height)
)
ORDER BY height;