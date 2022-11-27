-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT select_list 
    FROM table1 t_alias1 
    WHERE expr operator 
        (SELECT column_list 
            FROM table2 t_alias2 
            WHERE t_alias1.column 
               operator t_alias2.column); 

UPDATE table1 t_alias1 
    SET column = 
        (SELECT expr 
            FROM table2 t_alias2 
            WHERE t_alias1.column = t_alias2.column); 

DELETE FROM table1 t_alias1 
    WHERE column operator 
        (SELECT expr 
            FROM table2 t_alias2 
            WHERE t_alias1.column = t_alias2.column);