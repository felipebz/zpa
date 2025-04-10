-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/description-static-sql.html
DROP TABLE employees_temp;
CREATE TABLE employees_temp AS
  SELECT employee_id, first_name, last_name
  FROM employees;
DROP TABLE employees_temp2;
CREATE TABLE employees_temp2 AS
  SELECT employee_id, first_name, last_name
  FROM employees;
DECLARE
  seq_value NUMBER;
BEGIN
  -- Generate initial sequence number

  seq_value := employees_seq.NEXTVAL;

  -- Print initial sequence number:

  DBMS_OUTPUT.PUT_LINE (
    'Initial sequence value: ' || TO_CHAR(seq_value)
  );

  -- Use NEXTVAL to create unique number when inserting data:

     INSERT INTO employees_temp (employee_id, first_name, last_name) 
     VALUES (employees_seq.NEXTVAL, 'Lynette', 'Smith');

  -- Use CURRVAL to store same value somewhere else:

     INSERT INTO employees_temp2 VALUES (employees_seq.CURRVAL,
                                         'Morgan', 'Smith');

  /* Because NEXTVAL values might be referenced
     by different users and applications,
     and some NEXTVAL values might not be stored in database,
     there might be gaps in sequence. */

  -- Use CURRVAL to specify record to delete:

     seq_value := employees_seq.CURRVAL;

     DELETE FROM employees_temp2
     WHERE employee_id = seq_value;

  -- Update employee_id with NEXTVAL for specified record:

     UPDATE employees_temp
     SET employee_id = employees_seq.NEXTVAL
     WHERE first_name = 'Lynette'
     AND last_name = 'Smith';

  -- Display final value of CURRVAL:

     seq_value := employees_seq.CURRVAL;

     DBMS_OUTPUT.PUT_LINE (
       'Ending sequence value: ' || TO_CHAR(seq_value)
     );
END;
/