-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-property-graph.html
CREATE PROPERTY GRAPH “myGraph”
  VERTEX TABLES ( 
    HR.VT1, 
    VT1  AS ALTVT1,
    VT2  LABEL “foo” ,
    VT3  NO PROPERTIES,
    VT4  PROPERTIES(C1), 
    VT5  PROPERTIES(C1, C2 as P2),
    VT6  LABEL “bar” LABEL “weighted” NO PROPERTIES,
    VT7  LABEL “bar2” ALL COLUMNS ARE PROPERTIES EXCEPT (C3),
    VT8  LABEL “weighted” NO PROPERTIES DEFAULT LABEL,
    VT9  PROPERTIES(Cx + Cy * 0.15 AS PX, Cz AS PZ),
    VT10 PROPERTIES(JSON_VALUE(JCOL, 
      ‘$.person.creditScore[0]’ returning number) AS CREDITSCORE,
    VT11 PROPERTIES(XMLCAST(XMLQUERY(‘/purchaseOrder/poDate’ 
       PASSING XCOL RETURNING CONTENT) AS DATE) AS PURCHASEDATE
 );