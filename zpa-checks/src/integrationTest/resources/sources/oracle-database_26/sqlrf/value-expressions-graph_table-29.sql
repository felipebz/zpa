-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH path1 = (p1 IS person) -[IS friends]-{2} (p2 IS person),
             path2 = (p2) -[IS student_of]-> (u2 IS university)
       WHERE p1.name = 'Bob'
       ONE ROW PER VERTEX (v)
       COLUMNS (MATCHNUM() AS matchnum,
                PATH_NAME() AS path_name,
                ELEMENT_NUMBER(v) AS element_number,
                v.name))
ORDER BY matchnum, path_name, element_number;