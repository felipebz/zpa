-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN hourly_wages AS NUMBER(10)
       DEFAULT ON NULL 15
       CONSTRAINT minimal_wage_c
         CHECK (hourly_wages >= 7 and hourly_wages <=1000) ENABLE
       DISPLAY TO_CHAR(hourly_wages, '$999.99')
       ORDER ( -1*hourly_wages )
       ANNOTATIONS (Title 'Domain Annotation');