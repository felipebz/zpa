-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/value-expressions-graph_table.html
SELECT name
FROM GRAPH_TABLE ( students_graph
  MATCH (p IS person)
          -[IS friends]- (friend IS person)
            -[IS friends]- (friend_of_friend IS person)
  WHERE p.name = 'Mary' AND NOT vertex_equal(p, friend_of_friend)
  COLUMNS (friend_of_friend.name)
)
ORDER BY name;