-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/expressions.html
DECLARE
    salary NUMBER := 7000;
    salary_level VARCHAR2(20);
BEGIN
    salary_level :=
	CASE salary
		WHEN 1000, 2000 THEN 'low'
		WHEN 3000, 4000, 5000 THEN 'normal'
		WHEN 6000, 7000, 8000 THEN 'high'
		ELSE 'executive pay'
	END;
	DBMS_OUTPUT.PUT_LINE('Salary level is: ' || salary_level);
END;
/