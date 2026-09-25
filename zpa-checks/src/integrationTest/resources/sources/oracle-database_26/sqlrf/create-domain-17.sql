-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html
CREATE TABLE tax_report(id NUMBER, income JSON DOMAIN w2_form);