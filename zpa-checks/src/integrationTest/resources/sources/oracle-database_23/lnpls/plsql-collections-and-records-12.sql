-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
DECLARE
  TYPE dnames_tab IS TABLE OF VARCHAR2(30);

  dept_names dnames_tab := dnames_tab(
    'Shipping','Sales','Finance','Payroll');  -- Initialized to non-null value

  empty_set dnames_tab;  -- Not initialized, therefore null

  PROCEDURE print_dept_names_status IS
  BEGIN
    IF dept_names IS NULL THEN
      DBMS_OUTPUT.PUT_LINE('dept_names is null.');
    ELSE
      DBMS_OUTPUT.PUT_LINE('dept_names is not null.');
    END IF;
  END  print_dept_names_status;

BEGIN
  print_dept_names_status;
  dept_names := empty_set;  -- Assign null collection to dept_names.
  print_dept_names_status;
  dept_names := dnames_tab (
    'Shipping','Sales','Finance','Payroll');  -- Re-initialize dept_names
  print_dept_names_status;
END;
/