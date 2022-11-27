-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Sequence-Pseudocolumns.html
INSERT INTO employees
  VALUES (employees_seq.nextval, 'John', 'Doe', 'jdoe', '555-1212',
          TO_DATE(SYSDATE), 'PU_CLERK', 2500, null, null, 30);