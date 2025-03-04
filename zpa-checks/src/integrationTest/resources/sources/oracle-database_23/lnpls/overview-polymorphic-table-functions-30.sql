-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
CREATE PACKAGE BODY implicit_echo_package AS

FUNCTION DESCRIBE(tab  IN  OUT DBMS_TF.TABLE_T,
                  cols IN      DBMS_TF.COLUMNS_T)
          RETURN DBMS_TF.DESCRIBE_T
AS
  new_cols DBMS_TF.COLUMNS_NEW_T;
  col_id   PLS_INTEGER := 1;

BEGIN
 FOR i in 1 .. tab.column.COUNT LOOP

   FOR j in 1 .. cols.COUNT LOOP

     IF (tab.column(i).description.name = cols(j)) THEN

       IF (NOT DBMS_TF.SUPPORTED_TYPE(tab.column(i).description.type)) THEN
            RAISE_APPLICATION_ERROR(-20102, 'Unsupported column type['||
                                    tab.column(i).description.type||']');
       END IF;

       tab.column(i).for_read := TRUE;
       new_cols(col_id)       := tab.column(i).description;
       new_cols(col_id).name  := prefix||
                                 REGEXP_REPLACE(tab.column(i).description.name,
                                                                      '^"|"$');
       col_id                 := col_id + 1;
       EXIT;

     END IF;

    END LOOP;

 END LOOP;

/* VERIFY ALL COLUMNS WERE FOUND */
 IF (col_id - 1 != cols.COUNT) then
    RAISE_APPLICATION_ERROR(-20101,'Column mismatch['||col_id-1||'],
                                                   ['||cols.COUNT||']');
 END IF;

 RETURN DBMS_TF.DESCRIBE_T(new_columns => new_cols);

END;

 PROCEDURE FETCH_ROWS AS
	 rowset DBMS_TF.ROW_SET_T;
 BEGIN
         DBMS_TF.GET_ROW_SET(rowset);
         DBMS_TF.PUT_ROW_SET(rowset);
 END;

END implicit_echo_package;