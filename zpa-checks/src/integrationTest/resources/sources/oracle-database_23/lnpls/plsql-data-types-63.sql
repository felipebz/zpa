-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
  SUBTYPE Balance IS NUMBER(8,2);

  checking_account  Balance;
  savings_account   Balance;

BEGIN
  checking_account := 2000.00;
  savings_account  := 1000000.00;
END;
/