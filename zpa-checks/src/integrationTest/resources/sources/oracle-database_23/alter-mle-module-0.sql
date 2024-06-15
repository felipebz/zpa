-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/alter-mle-module.html
ALTER MLE MODULE myMLEModule 
SET METADATA USING CLOB (
SELECT JSON(
  '{
       "name": "value",
       "version": "1.2.0",
       "commitHash": "23fas4h",
       "projectName": "Database Backend"
  }')
)