-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( students_graph
       MATCH (x IS person|university)
       COLUMNS (CASE
                  WHEN x IS LABELED person THEN 'PERSON'
                  ELSE 'UNIVERSITY'
                END AS label,
                PROPERTY_EXISTS(x, dob) AS has_dob,
                PROPERTY_EXISTS(x, height) AS has_height, 
                PROPERTY_EXISTS(x, name) AS has_name,
                PROPERTY_EXISTS(x, id) AS has_id))
GROUP BY label, has_dob, has_height, has_name, has_id
ORDER BY label;