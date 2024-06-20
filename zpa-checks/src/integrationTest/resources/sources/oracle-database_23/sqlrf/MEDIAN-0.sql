-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MEDIAN.html
if (CRN = FRN = RN) then
      (value of expression from row at RN)
   else
      (CRN - RN) * (value of expression for row at FRN) +
      (RN - FRN) * (value of expression for row at CRN)