-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
CREATE PACKAGE to_doc_p AS
   FUNCTION describe(tab      IN OUT DBMS_TF.TABLE_T,
                     cols     IN     DBMS_TF.COLUMNS_T DEFAULT NULL)
		       RETURN DBMS_TF.DESCRIBE_T;

   PROCEDURE fetch_rows;
END to_doc_p;