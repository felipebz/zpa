-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE FUNCTION take (n NUMBER, t DBMS_TF.table_t) 
                      RETURN VARCHAR2 SQL_MACRO IS
BEGIN
   RETURN 'SELECT * FROM t FETCH FIRST take.n ROWS ONLY';
END;
/