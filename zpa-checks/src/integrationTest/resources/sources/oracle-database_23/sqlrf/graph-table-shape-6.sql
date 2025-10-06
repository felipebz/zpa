-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-table-shape.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (n IS person) -[e1 IS friends]->{0,3} (IS person)
       WHERE n.name = 'John'
       ONE ROW PER VERTEX (v)
       COLUMNS (
        LISTAGG(e1.friendship_id, ', ') AS friendship_ids,
        v.name)
     );