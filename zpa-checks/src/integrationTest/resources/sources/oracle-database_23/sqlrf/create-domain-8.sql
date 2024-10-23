-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN dept_codes AS NUMBER(3) STRICT
      CONSTRAINT dept_chk CHECK (dept_codes > 99 AND dept_codes != 200) 
      ANNOTATIONS (Title 'Domain Annotation');