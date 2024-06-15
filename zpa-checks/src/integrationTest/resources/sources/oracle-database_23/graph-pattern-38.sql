-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (u1 IS university) -[e]-{,3} (u2 IS university)
       WHERE u1.name = 'ABC' AND u2.name = 'XYZ'
       COLUMNS (JSON_ARRAYAGG(CASE WHEN e.subject IS NOT NULL THEN e.subject
                              ELSE CAST(e.friendship_id AS VARCHAR(100)) END) AS path));