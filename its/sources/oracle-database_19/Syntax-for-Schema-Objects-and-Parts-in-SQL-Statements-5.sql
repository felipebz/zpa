-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html
SELECT c.cust_address.postal_code
  FROM customers c;

UPDATE customers c
  SET c.cust_address.postal_code = '14621-2604'
  WHERE c.cust_address.city = 'Rochester'
    AND c.cust_address.state_province = 'NY';