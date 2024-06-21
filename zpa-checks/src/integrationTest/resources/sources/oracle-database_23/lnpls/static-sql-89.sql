-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/static-sql.html
INSERT INTO log (log_id, up_date, new_sal, old_sal)
VALUES (999, SYSDATE, 5000, 4500);
SELECT * FROM temp;