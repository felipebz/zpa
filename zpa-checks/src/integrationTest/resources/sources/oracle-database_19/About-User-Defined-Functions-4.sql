-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/About-User-Defined-Functions.html
SELECT hr.tax_rate (ss_no, sal)
    INTO income_tax
    FROM tax_table WHERE ss_no = tax_id;