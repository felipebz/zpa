-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/conditional-compilation1.html
CREATE OR REPLACE PROCEDURE my_proc (
  $IF $$xxx $THEN i IN PLS_INTEGER $ELSE i IN INTEGER $END
) IS 
BEGIN 
      NULL; 
END my_proc;