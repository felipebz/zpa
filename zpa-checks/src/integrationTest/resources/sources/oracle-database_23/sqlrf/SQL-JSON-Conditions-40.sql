-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
SELECT family_doc FROM families
  where json_textcontains(family_doc, '$.family.id', '10');