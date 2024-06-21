-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
SELECT w.column_value "weighted result" 
FROM pkg_gpa.weighted_average (
    CURSOR (SELECT weight, grade FROM gradereport)
  ) w;