-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/avoiding-inner-capture-select-and-dml-statements.html
UPDATE ot1 SET VALUE(ot1.x) = t1(20) WHERE VALUE(ot1.x) = t1(10);
DELETE FROM ot1 WHERE VALUE(ot1) = (t1(10));