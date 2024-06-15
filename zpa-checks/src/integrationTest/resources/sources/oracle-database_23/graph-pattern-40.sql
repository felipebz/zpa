-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT DISTINCT name, birthday
FROM GRAPH_TABLE ( students_graph
  MATCH
   (a IS person)
     ( (x) -[e IS friends]- (y IS person) 
       WHERE x.dob < y.dob ){1,3}
         (b IS person)
  WHERE a.name = 'Bob'
  COLUMNS (b.name, b.dob AS birthday)
)
ORDER BY birthday;