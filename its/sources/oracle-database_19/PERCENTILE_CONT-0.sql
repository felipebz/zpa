-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/PERCENTILE_CONT.html
If (CRN = FRN = RN) then the result is
    (value of expression from row at RN)
  Otherwise the result is
    (CRN - RN) * (value of expression for row at FRN) +
    (RN - FRN) * (value of expression for row at CRN)