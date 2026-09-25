-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html
CREATE OR REPLACE PACKAGE P AS
  FUNCTION describe (tab IN OUT DBMS_TF.table_t)
    RETURN DBMS_TF.describe_t;
END P;
/
CREATE OR REPLACE PACKAGE BODY P AS
  FUNCTION describe (tab IN OUT DBMS_TF.table_t)
    RETURN DBMS_TF.describe_t
  AS
  BEGIN
    RETURN NULL;
  END;
END P;
/
CREATE OR REPLACE FUNCTION F(t IN TABLE)
  RETURN TABLE PIPELINED
  ROW POLYMORPHIC USING P;
/