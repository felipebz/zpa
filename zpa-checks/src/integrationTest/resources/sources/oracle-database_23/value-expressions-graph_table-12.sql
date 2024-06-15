-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p1 IS person) -[e IS friends]- (p2 IS person)
       WHERE p1.name = 'Mary'
       COLUMNS (e.friendship_id,
                e.meeting_date,
                CASE WHEN p1 IS SOURCE OF e THEN p1.name ELSE p2.name END AS from_person,
                CASE WHEN p1 IS DESTINATION OF e THEN p1.name ELSE p2.name END AS to_person))
ORDER BY friendship_id;