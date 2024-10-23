-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN meals_details AS
    (
       restaurant AS VARCHAR2(100) NOT NULL,
       meal_type AS VARCHAR2(200),
       diner_count AS NUMBER
    )
    CONSTRAINT meals_c
      CHECK
       (
         restaurant IS NOT NULL AND
         meal_type IN ('Breakfast', 'Lunch', 'Dinner') AND
         diner_count IS NOT NULL
       )
    DISPLAY meal_type||', '||restaurant||', '||diner_count;