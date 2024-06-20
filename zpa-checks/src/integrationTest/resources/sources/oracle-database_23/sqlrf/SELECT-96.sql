-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
UPDATE table1 t_alias1 
    SET column = 
        (SELECT expr 
            FROM table2 t_alias2 
            WHERE t_alias1.column = t_alias2.column);