-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE BLOCKCHAIN TABLE bank_ledger (bank VARCHAR2(128), account_no NUMBER, deposit_date DATE, deposit_amount NUMBER)
         NO DROP UNTIL 31 DAYS IDLE
         NO DELETE LOCKED
         HASHING USING SHA2_512 WITH ROW VERSION AND USER CHAIN bank_accounts (bank, account_no) VERSION V2;