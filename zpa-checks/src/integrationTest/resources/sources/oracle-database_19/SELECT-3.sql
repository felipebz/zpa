-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
a NATURAL LEFT JOIN (b LEFT JOIN c ON b.c1 = c.c1) 
   (a NATURAL LEFT JOIN b) LEFT JOIN c ON b.c1 = c.c1