-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE Employee (
  Id  NUMBER(5) ANNOTATIONS(Identity, Display ’Employee ID’, Group ’Emp_Info’),
  Ename VARCHAR2(50) ANNOTATIONS(Display ’Employee Name’,  Group ’Emp_Info’),
  Sal NUMBER TAG ANNOTATIONS(Display ’Employee Salary’, UI_Hidden)
) ANNOTATIONS (Display ’Employee Table’);