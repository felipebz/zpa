-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “myGraph”
    VERTEX TABLES ( 
      “mytable1” LABEL “foo” LABEL “weighted”, 
      “mytable2” LABEL “weighted”), 
    EDGE TABLES ( 
      "E1" SOURCE  “mytable1” DESTINATION “mytable2” LABEL “weighted”
      "E2" SOURCE  “mytable2” DESTINATION “mytable1” LABEL “weighted”
  );