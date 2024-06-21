-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
CREATE PACKAGE gen IS 
   FUNCTION range(stop NUMBER)
            RETURN VARCHAR2 SQL_MACRO(TABLE);

   FUNCTION range(first NUMBER DEFAULT 0, stop NUMBER, step NUMBER DEFAULT 1)
            RETURN VARCHAR2 SQL_MACRO(TABLE);

   FUNCTION tab(tab TABLE, replication_factor NATURAL)
            RETURN TABLE PIPELINED ROW POLYMORPHIC USING gen;

   FUNCTION describe(tab IN OUT DBMS_TF.TABLE_T, replication_factor NATURAL)
            RETURN DBMS_TF.DESCRIBE_T;

   PROCEDURE fetch_rows(replication_factor NATURALN);
END gen;
/
CREATE PACKAGE BODY gen IS 
   FUNCTION range(stop NUMBER)
            RETURN VARCHAR2 SQL_MACRO(TABLE) IS
   BEGIN 
      RETURN q'{SELECT ROWNUM-1 n FROM gen.tab(DUAL, stop)}'; 
   END;

   FUNCTION range(first NUMBER DEFAULT 0, stop NUMBER, step NUMBER DEFAULT 1)
           RETURN VARCHAR2 SQL_MACRO(TABLE) IS
   BEGIN
      RETURN q'{
             SELECT first+n*step n FROM gen.range(ROUND((stop-first)/NULLIF(step,0)))
             }';
   END;

   FUNCTION describe(tab IN OUT DBMS_TF.TABLE_T, replication_factor NATURAL) 
            RETURN DBMS_TF.DESCRIBE_T AS
   BEGIN 
      RETURN DBMS_TF.DESCRIBE_T(row_replication => true);
   END;

  PROCEDURE fetch_rows(replication_factor NATURALN) as
  BEGIN 
    DBMS_TF.ROW_REPLICATION(replication_factor); 
  END;
END gen;
/