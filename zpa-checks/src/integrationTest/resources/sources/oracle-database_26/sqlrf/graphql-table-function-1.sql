-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/graphql-table-function.html
  SELECT * FROM GRAPHQL
('
    employees {
		  _id: employee_id
		  Name: first_name
	       }
');