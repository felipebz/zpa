-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT *
FROM GRAPH_TABLE ( persons_graph
       MATCH (n)
       WHERE n.person_data.department = 'HR'
       COLUMNS (n.name, n.person_data.role.string() AS role)
     );