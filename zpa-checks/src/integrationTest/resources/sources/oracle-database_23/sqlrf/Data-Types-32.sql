-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
CREATE TABLE t (v VECTOR);
CREATE TABLE t (v VECTOR(*, *));
CREATE TABLE t (v VECTOR(100));
CREATE TABLE t (v VECTOR(100, *));
CREATE TABLE t (v VECTOR(*, FLOAT32));
CREATE TABLE t (v VECTOR(100, FLOAT32));