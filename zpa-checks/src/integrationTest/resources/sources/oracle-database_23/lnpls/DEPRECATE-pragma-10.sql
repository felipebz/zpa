-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE trans_data AUTHID DEFINER AS
   TYPE Transrec IS RECORD (
     accounttype VARCHAR2(30) ,
     ownername VARCHAR2(30) ,
     balance REAL
   );
   min_balance constant real := 10.0;
   PRAGMA DEPRECATE(min_balance , 'Minimum balance requirement has been removed.');
   insufficient_funds EXCEPTION;
   PRAGMA DEPRECATE (insufficient_funds , 'Exception no longer raised.');
END trans_data;