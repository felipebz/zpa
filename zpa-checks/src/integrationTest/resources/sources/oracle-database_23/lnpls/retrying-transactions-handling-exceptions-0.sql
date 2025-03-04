-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/retrying-transactions-handling-exceptions.html
DROP TABLE results;
CREATE TABLE results (
  res_name   VARCHAR(20),
  res_answer VARCHAR2(3)
);
CREATE UNIQUE INDEX res_name_ix ON results (res_name);
INSERT INTO results (res_name, res_answer) VALUES ('SMYTHE', 'YES');
INSERT INTO results (res_name, res_answer) VALUES ('JONES', 'NO');
DECLARE
  name    VARCHAR2(20) := 'SMYTHE';
  answer  VARCHAR2(3) := 'NO';
  suffix  NUMBER := 1;
BEGIN
  FOR i IN 1..5 LOOP  -- Try transaction at most 5 times.

    DBMS_OUTPUT.PUT('Try #' || i);

    BEGIN  -- sub-block begins

       SAVEPOINT start_transaction;

       -- transaction begins

       DELETE FROM results WHERE res_answer = 'NO';

       INSERT INTO results (res_name, res_answer) VALUES (name, answer);

       -- Nonunique name raises DUP_VAL_ON_INDEX.

       -- If transaction succeeded:

       COMMIT;
       DBMS_OUTPUT.PUT_LINE(' succeeded.');
       EXIT;

    EXCEPTION
      WHEN DUP_VAL_ON_INDEX THEN
        DBMS_OUTPUT.PUT_LINE(' failed; trying again.');
        ROLLBACK TO start_transaction;    -- Undo changes.
        suffix := suffix + 1;             -- Try to fix problem.
        name := name || TO_CHAR(suffix);
    END;  -- sub-block ends

  END LOOP;
END;
/