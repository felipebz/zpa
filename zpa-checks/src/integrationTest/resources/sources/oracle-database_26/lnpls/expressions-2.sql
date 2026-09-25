-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/expressions.html
BEGIN
  DBMS_OUTPUT.PUT_LINE ('apple' || NULL || NULL || 'sauce');
END;
/