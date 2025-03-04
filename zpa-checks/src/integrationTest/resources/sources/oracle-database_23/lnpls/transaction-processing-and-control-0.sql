-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/transaction-processing-and-control.html
DROP TABLE accounts;
CREATE TABLE accounts (
  account_id  NUMBER(6),
  balance     NUMBER (10,2)
);
INSERT INTO accounts (account_id, balance)
VALUES (7715, 6350.00);
INSERT INTO accounts (account_id, balance)
VALUES (7720, 5100.50);
CREATE OR REPLACE PROCEDURE transfer (
  from_acct  NUMBER,
  to_acct    NUMBER,
  amount     NUMBER
) AUTHID CURRENT_USER AS
BEGIN
  UPDATE accounts
  SET balance = balance - amount
  WHERE account_id = from_acct;

  UPDATE accounts
  SET balance = balance + amount
  WHERE account_id = to_acct;

  COMMIT WRITE IMMEDIATE NOWAIT;
END;
/