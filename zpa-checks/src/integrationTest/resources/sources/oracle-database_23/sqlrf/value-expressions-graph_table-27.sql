-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p1 IS person) -[IS friends]-{0,1} (p2 IS person)
       WHERE p1.name = 'John'
       ONE ROW PER STEP (v1, e, v2)
       COLUMNS (MATCHNUM() AS matchnum,
                ELEMENT_NUMBER(e) AS element_number,
                v1.name AS name1,
                e.friendship_id,
                v2.name AS name2))
ORDER BY matchnum, element_number;