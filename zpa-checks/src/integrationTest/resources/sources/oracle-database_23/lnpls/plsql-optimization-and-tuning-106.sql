-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
CREATE PACKAGE BODY skip_col_pkg AS

/* OVERLOAD 1: Skip by name 
 * Package PTF name:  skip_col_pkg.skip_col
 * Standalone PTF name: skip_col_by_name
 *
 * PARAMETERS:
 * tab - The input table
 * col - The name of the columns to drop from the output
 *
 * DESCRIPTION:
 *   This PTF removes all the input columns listed in col from the output
 *   of the PTF.
*/  
 FUNCTION  describe(tab IN OUT DBMS_TF.TABLE_T, 
                    col        DBMS_TF.COLUMNS_T)
            RETURN DBMS_TF.DESCRIBE_T
  AS 
    new_cols DBMS_TF.COLUMNS_NEW_T;
    col_id   PLS_INTEGER := 1;
  BEGIN 
    FOR i IN 1 .. tab.column.count() LOOP
      FOR j IN 1 .. col.count() LOOP
        tab.column(i).PASS_THROUGH := tab.column(i).DESCRIPTION.NAME != col(j);
        EXIT WHEN NOT tab.column(i).PASS_THROUGH;
      END LOOP;
    END LOOP;

    RETURN NULL;
  END;  

/* OVERLOAD 2: Skip by type
 * Package PTF name:  skip_col_pkg.skip_col
 * Standalone PTF name: skip_col_by_type
 *
 * PARAMETERS:
 *   tab       - Input table
 *   type_name - A string representing the type of columns to skip
 *   flip      - 'False' [default] => Match columns with given type_name
 *               otherwise         => Ignore columns with given type_name
 *
 * DESCRIPTION:
 *   This PTF removes the given type of columns from the given table. 
*/ 
  FUNCTION describe(tab       IN OUT DBMS_TF.TABLE_T, 
                    type_name        VARCHAR2, 
                    flip             VARCHAR2 DEFAULT 'False') 
           RETURN DBMS_TF.DESCRIBE_T 
  AS 
    typ CONSTANT VARCHAR2(1024) := UPPER(TRIM(type_name));
  BEGIN 
    FOR i IN 1 .. tab.column.count() LOOP
       tab.column(i).PASS_THROUGH := 
         CASE UPPER(SUBSTR(flip,1,1))
           WHEN 'F' THEN DBMS_TF.column_type_name(tab.column(i).DESCRIPTION)!=typ
           ELSE          DBMS_TF.column_type_name(tab.column(i).DESCRIPTION) =typ
         END /* case */;
    END LOOP;

    RETURN NULL;
  END;

END skip_col_pkg;