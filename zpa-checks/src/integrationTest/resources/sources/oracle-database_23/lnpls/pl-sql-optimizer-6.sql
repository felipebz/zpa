-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pl-sql-optimizer.html
x:= p1(1) + p1(2) + 17;    -- These 2 invocations to p1 are not inlined
