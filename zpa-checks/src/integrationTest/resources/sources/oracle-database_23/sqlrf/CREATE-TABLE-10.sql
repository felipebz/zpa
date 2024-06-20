-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE SHARDED TABLE departments
    ( department_id  NUMBER(6)
    , department_name VARCHAR2(30) CONSTRAINT dept_name_nn NOT NULL
    , manager_id    NUMBER(6)
    , location_id   NUMBER(4)
    , CONSTRAINT dept_id_pk PRIMARY KEY(department_id)
    )
    PARTITION BY DIRECTORY (department_id)
    (
      PARTITION p_1 TABLESPACE tbs1,
      PARTITION p_2 TABLESPACE tbs2
    );