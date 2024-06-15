-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGR_-Linear-Regression-Functions.html
NULL if VAR_POP(expr2)  = 0

                        1 if VAR_POP(expr1)  = 0 and 
                             VAR_POP(expr2) != 0

POWER(CORR(expr1,expr),2) if VAR_POP(expr1)  > 0 and
                             VAR_POP(expr2  != 0