-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CONSTRAINT_NAME                          SEARCH_CONDITION                    PRECHECK
__________________                       ___________________________________ _____________
SYS_C008676 "ID"                         IS NOT NULL
SYS_C008677 "CATEGORY"                   IS NOT NULL
SYS_C008678  mod(price,4)                = 0 and 10 <> price                  PRECHECK
SYS_C008679 Color                        >= 10 and Color <=50                 PRECHECK
SYS_C008680 Length(Description)          <= 40                                PRECHECK
TC1                                      Color > 0 AND Price > 10             PRECHECK
TC2                                      CATEGORY IN ('Home', 'Apparel')      NOPRECHECK
TC3                                      Created_At > Updated_At              NOPRECHECK

8 rows selected.