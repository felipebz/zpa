-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/lexical-units.html
BEGIN
  DBMS_OUTPUT.PUT_LINE('This string ' ||
                       'contains no line-break character.');
END;
/