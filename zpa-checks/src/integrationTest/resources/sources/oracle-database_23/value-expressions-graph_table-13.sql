-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p1 IS person) -[e1 IS friends]- (p2 IS person)
               -[e2 IS friends]- (p3 IS person)
       WHERE p1.name = 'John'
         AND ((p1 IS SOURCE OF e1 AND p2 IS SOURCE OF e2) OR
             (p1 IS DESTINATION OF e1 AND p2 IS DESTINATION OF e2))
       COLUMNS (p1.name AS person_1,
                CASE WHEN p1 IS SOURCE OF e1
                  THEN 'Outgoing' ELSE 'Incoming'
                  END AS e1_direction,
                p2.name AS person_2,
                CASE WHEN p2 IS SOURCE OF e2
                  THEN 'Outgoing' ELSE 'Incoming'
                  END AS e2_direction,
                p3.name AS person_3))
ORDER BY 1, 2, 3;