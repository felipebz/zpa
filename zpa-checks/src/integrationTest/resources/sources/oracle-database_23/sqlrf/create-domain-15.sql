-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE FLEXIBLE DOMAIN expense_details (val1, val2, val3, val4)
    CHOOSE DOMAIN USING(typ VARCHAR2(10))
    FROM CASE
        WHEN typ BETWEEN 'A' AND 'G' THEN flight_details(val1, val2, val3)
        WHEN typ = 'Meals' THEN meals_details(val1, val2, val4)
        WHEN typ LIKE 'Lodg%' THEN lodging_details(val1, val4)
      END;