-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/expressions.html
DECLARE
    data_val NUMBER := 30;
    status VARCHAR2(20);
BEGIN
    status :=
	CASE data_val/2
		WHEN < 0, > 50 THEN 'outlier'
		WHEN BETWEEN 10 AND 30 THEN 'good'
		ELSE 'bad'
	END;
	DBMS_OUTPUT.PUT_LINE('The data status is: ' || status);
END;
/