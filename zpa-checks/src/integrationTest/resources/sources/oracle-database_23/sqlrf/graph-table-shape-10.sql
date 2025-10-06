-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (u1 IS university)
               <-[IS student_of]- (p1 IS person)
               -[IS friends]-{1,2} (p2 IS person)
               -[IS student_of]-> (u2 IS university)
       WHERE u1.name = 'ABC' AND u2.name = 'XYZ'
       ONE ROW PER VERTEX (v)
       COLUMNS (MATCHNUM() AS matchnum,
                ELEMENT_NUMBER(v) AS element_number,
                CASE WHEN v.person_id IS NOT NULL
                  THEN 'person'
                  ELSE 'university'
                  END AS label,
                v.name))
ORDER BY matchnum, element_number;