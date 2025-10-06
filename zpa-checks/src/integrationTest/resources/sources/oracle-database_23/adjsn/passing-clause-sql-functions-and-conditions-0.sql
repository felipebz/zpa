-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/passing-clause-sql-functions-and-conditions.html
SELECT count(*) FROM j_purchaseorder
WHERE json_exists(data, '$.PONumber?(@.numberOnly() > $d)'
PASSING to_number(:1) AS "d");