-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graphql-table-function.html
  CREATE OR REPLACE JSON RELATIONAL DUALITY VIEW student_ov AS 
    student { # this is a valid single line GraphQL comment
    _id: stuid
    Name: name
    -- SQL comments can be placed within GraphQL expression as well
};