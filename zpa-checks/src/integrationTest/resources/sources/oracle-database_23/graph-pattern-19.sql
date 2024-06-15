-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph-pattern.html
SELECT *
FROM GRAPH_TABLE ( students_graph
  MATCH -[e IS friends WHERE e.meeting_date > DATE '2001-01-01']->
  COLUMNS (e.meeting_date)
);