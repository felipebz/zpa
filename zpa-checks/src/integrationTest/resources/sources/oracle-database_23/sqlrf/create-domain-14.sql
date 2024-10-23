-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE FLEXIBLE DOMAIN expense_details (val1, val2, val3, val4)
    CHOOSE DOMAIN USING (typ VARCHAR2(10))
    FROM DECODE(typ,
              'Flight', flight_details(val1, val2, val3),
              'Meals', meals_details(val1, val2, val4),
              'Lodging', lodging_details(val1, val4));