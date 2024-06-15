-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (p IS person) -[e IS friends]-{2,5} (friend IS person)
       WHERE p.name = 'Alice' AND
             COUNT(e.friendship_id) = COUNT(DISTINCT e.friendship_id)
       COLUMNS (LISTAGG(e.friendship_id, ', ') AS friendship_ids,
                COUNT(e.friendship_id) AS path_length));