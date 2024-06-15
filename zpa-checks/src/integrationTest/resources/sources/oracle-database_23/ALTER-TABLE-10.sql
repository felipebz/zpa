-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE imm_tab NO DELETE UNTIL 120 DAYS AFTER
    INSERT;