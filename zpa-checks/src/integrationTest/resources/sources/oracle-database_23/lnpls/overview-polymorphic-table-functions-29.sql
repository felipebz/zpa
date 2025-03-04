-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
CREATE PACKAGE implicit_echo_package AS
  prefix   DBMS_ID := '"ECHO_';

  FUNCTION DESCRIBE(tab   IN OUT DBMS_TF.TABLE_T,
                    cols  IN     DBMS_TF.COLUMNS_T)
           RETURN DBMS_TF.DESCRIBE_T;

  PROCEDURE FETCH_ROWS;

  -- PTF FUNCTION: WITHOUT USING CLAUSE --
  FUNCTION implicit_echo(tab TABLE, cols COLUMNS)
           RETURN TABLE PIPELINED ROW POLYMORPHIC;

END implicit_echo_package;