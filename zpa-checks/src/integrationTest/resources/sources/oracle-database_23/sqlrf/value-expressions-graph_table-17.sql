-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p IS person) ( -[e IS friends]-> (friend IS person)
                             WHERE p.person_id <> friend.person_id){2,3}
       WHERE p.name = 'John'
       COLUMNS (COUNT(edge_id(e)) AS path_length,
                LISTAGG(friend.name, ', ') AS names,
                LISTAGG(e.meeting_date, ', ') AS meeting_dates ))
ORDER BY path_length;