-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comments.html
select /*+ LEADING(t2) USE_NL(t1) */ sum(t1.a),sum(t2.a)
from   t1 , t2
where   t1.b = t2.b;
select * from table(dbms_xplan.display_cursor()) ;