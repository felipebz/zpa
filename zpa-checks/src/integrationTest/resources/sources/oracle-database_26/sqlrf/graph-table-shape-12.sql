-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH path1 = (u1 IS university) <-[IS student_of]- (p1 IS person),
             path2 = (p1) -[IS friends]-{1,2} (p2 IS person),
             path3 = (p2) -[IS student_of]-> (u2 IS university)
       WHERE u1.name = 'ABC' AND u2.name = 'XYZ'
       ONE ROW PER VERTEX (v) IN (path2)
       COLUMNS (MATCHNUM() AS matchnum,
                PATH_NAME() AS path_name,
                ELEMENT_NUMBER(v) AS element_number,
                v.*))
ORDER BY matchnum, element_number;