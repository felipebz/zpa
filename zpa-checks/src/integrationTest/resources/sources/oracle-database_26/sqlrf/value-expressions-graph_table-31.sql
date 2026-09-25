-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p1 IS person) -[IS friends|student_of]-> (x IS person|university)
       WHERE p1.name = 'Bob'
       COLUMNS (CASE
                  WHEN x IS LABELED person THEN 'PERSON'
                  ELSE 'UNIVERSITY'
                END AS label,
                CASE
                  WHEN x IS LABELED person THEN x.person_id
                  ELSE x.id
                END AS id,
                x.name))
ORDER BY label, id;