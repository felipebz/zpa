-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/annotations_clause.html
ALTER TABLE employee
  MODIFY ename ANNOTATIONS (
    DROP "Group",
    DROP IF EXISTS missing_annotation,
    REPLACE Display 'Emp name'
  );