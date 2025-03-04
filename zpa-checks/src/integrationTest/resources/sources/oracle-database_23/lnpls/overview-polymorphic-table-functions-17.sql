-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
CREATE PACKAGE BODY to_doc_p AS
   
FUNCTION describe(tab      IN OUT DBMS_TF.TABLE_T,
                  cols     IN     DBMS_TF.COLUMNS_T DEFAULT NULL)
		    RETURN DBMS_TF.DESCRIBE_T AS
BEGIN
  FOR i IN 1 .. tab.column.count LOOP 
	 CONTINUE WHEN NOT DBMS_TF.SUPPORTED_TYPE(tab.column(i).DESCRIPTION.TYPE);

	  IF cols IS NULL THEN
	     tab.column(i).FOR_READ     := TRUE;
	     tab.column(i).PASS_THROUGH := FALSE;
	     CONTINUE;
	   END IF;

	  FOR j IN 1 .. cols.count LOOP
	    IF (tab.column(i).DESCRIPTION.NAME = cols(j)) THEN
	        tab.column(i).FOR_READ     := TRUE;
	        tab.column(i).PASS_THROUGH := FALSE;
	    END IF;
	  END LOOP;

  END LOOP;

  RETURN DBMS_TF.describe_t(new_columns => DBMS_TF.COLUMNS_NEW_T(1 =>
	                          DBMS_TF.COLUMN_METADATA_T(name =>'DOCUMENT')));   
END;

 PROCEDURE fetch_rows AS 
      rst DBMS_TF.ROW_SET_T;
      col DBMS_TF.TAB_VARCHAR2_T;
      rct PLS_INTEGER;
 BEGIN
      DBMS_TF.GET_ROW_SET(rst, row_count => rct);
      FOR rid IN 1 .. rct LOOP 
	       col(rid) := DBMS_TF.ROW_TO_CHAR(rst, rid); 
      END LOOP;
      DBMS_TF.PUT_COL(1, col);
 END; 

END to_doc_p;