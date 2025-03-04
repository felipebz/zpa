-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/bulk-sql-and-bulk-binding.html
DECLARE
  TYPE NumTab IS TABLE OF employees.employee_id%TYPE;
  TYPE NameTab IS TABLE OF employees.last_name%TYPE;

  enums NumTab;
  names NameTab;

  PROCEDURE print_first_n (n POSITIVE) IS
  BEGIN
    IF enums.COUNT = 0 THEN
      DBMS_OUTPUT.PUT_LINE ('Collections are empty.');
    ELSE
      DBMS_OUTPUT.PUT_LINE ('First ' || n || ' employees:');

      FOR i IN 1 .. n LOOP
        DBMS_OUTPUT.PUT_LINE (
          '  Employee #' || enums(i) || ': ' || names(i));
      END LOOP;
    END IF;
  END;

BEGIN
  SELECT employee_id, last_name
  BULK COLLECT INTO enums, names
  FROM employees
  ORDER BY employee_id;

  print_first_n(3);
  print_first_n(6);
END;
/