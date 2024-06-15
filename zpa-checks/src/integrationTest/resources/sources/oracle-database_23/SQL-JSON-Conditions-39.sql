-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
SELECT family_doc FROM families
  WHERE JSON_TEXTCONTAINS(family_doc, '$', '10');