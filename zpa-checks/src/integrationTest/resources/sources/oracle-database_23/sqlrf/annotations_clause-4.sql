-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/annotations_clause.html
CREATE TABLE employee (
  id NUMBER(5)
    ANNOTATIONS(Identity, Display 'Employee ID', "Group" 'Emp_Info'),
  ename VARCHAR2(50)
    ANNOTATIONS(Display 'Employee Name', "Group" 'Emp_Info'),
  sal NUMBER
    ANNOTATIONS(Display 'Employee Salary', UI_Hidden)
) ANNOTATIONS (Display 'Employee Table');