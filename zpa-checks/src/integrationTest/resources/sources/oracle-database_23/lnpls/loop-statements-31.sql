-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
OPEN c FOR SELECT id, data FROM T;
   result(r.id) := r.data;
CLOSE c;