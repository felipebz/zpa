-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p IS person) -[e IS friends]-{2,5} (friend IS person)
       WHERE p.name = 'Alice' AND
             COUNT(edge_id(e)) = COUNT(DISTINCT edge_id(e))
       COLUMNS (LISTAGG(e.friendship_id, ', ') AS friendship_ids,
                COUNT(edge_id(e)) AS path_length))
ORDER BY path_length, friendship_ids;