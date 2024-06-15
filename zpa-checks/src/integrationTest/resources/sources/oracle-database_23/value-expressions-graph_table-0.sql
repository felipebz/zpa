-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT GT.name, GT.birthday
FROM GRAPH_TABLE ( students_graph
  MATCH (p IS person|university)
  COLUMNS (p.name, p.dob AS birthday)
) GT
ORDER BY GT.birthday, GT.name;