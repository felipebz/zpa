-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html
CREATE VIEW V2 AS
SELECT aid, bid, rownum AS ID FROM GRAPH_TABLE (G1
MATCH (a)-[IS friend_of]->{1,10}(b)
COLUMNS (a.id AS aid, b.id AS bid));
CREATE PROPERTY GRAPH G2
VERTEX TABLES(
V1 AS EMPLOYEE
KEY(ID)
LABEL EMP
PROPERTIES ARE ALL COLUMNS
)
EDGE TABLES(
V2 AS can_reach KEY(ID)
SOURCE KEY(aid) REFERENCES EMPLOYEE(ID)
DESTINATION KEY(bid) REFERENCES EMPLOYEE(ID)
NO PROPERTIES
);