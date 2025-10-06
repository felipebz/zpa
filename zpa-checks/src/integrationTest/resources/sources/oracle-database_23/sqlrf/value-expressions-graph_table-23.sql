-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p1 IS person) -[IS friends]-{1,2} (p2 IS person)
       WHERE p1.name = 'John' AND p2.name = 'Mary'
       ONE ROW PER VERTEX (v)
       COLUMNS (MATCHNUM() AS matchnum,
                ELEMENT_NUMBER(v) AS element_number,
                v.name))
ORDER BY matchnum, element_number;