-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT name, birthday
FROM GRAPH_TABLE ( students_graph
  MATCH (p IS person WHERE p.dob > DATE '1980-01-01')
  COLUMNS (p.name, p.dob AS birthday)
)
ORDER BY birthday;