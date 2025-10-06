-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE IMMUTABLE TABLE trade_ledger (tr_id NUMBER, user_name VARCHAR2(40), tr_value NUMBER)

       NO DROP UNTIL 40 DAYS IDLE   

       NO DELETE UNTIL 100 DAYS AFTER INSERT;