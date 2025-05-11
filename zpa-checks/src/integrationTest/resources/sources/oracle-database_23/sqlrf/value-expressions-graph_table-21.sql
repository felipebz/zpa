-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p IS person)
       COLUMNS (MATCHNUM() AS matchnum,
                p.name))
ORDER BY matchnum;