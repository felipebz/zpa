-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/avoiding-inner-capture-select-and-dml-statements.html
BEGIN
  INSERT INTO tb1 VALUES ( t1(10) );
  INSERT INTO tb1 VALUES ( t1(20) );
  INSERT INTO tb1 VALUES ( t1(30) );
END;
/