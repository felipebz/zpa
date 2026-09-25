-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH "mygraph"
  VERTEX TABLES ("myschema". "mytable" LABEL “foo” LABEL “bar”);