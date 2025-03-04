-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/invokers-rights-and-definers-rights-authid-property.html
CREATE OR REPLACE PROCEDURE hr_remote_db_link
AS
v_employee_id VARCHAR(50);
BEGIN  
    EXECUTE IMMEDIATE 'SELECT employee_id FROM employees@dblink' into v_employee_id;
    DBMS_OUTPUT.PUT_LINE('employee_id: ' || v_employee_id);
END ;
/