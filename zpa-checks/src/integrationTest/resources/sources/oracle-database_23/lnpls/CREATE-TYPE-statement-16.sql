-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE OR REPLACE TYPE BODY employee_t IS
   STATIC FUNCTION construct_emp
   (name varchar2, dept REF department_t)
   RETURN employee_t IS
      BEGIN
         return employee_t(SYS_GUID(),name,dept);
      END;
END;
/