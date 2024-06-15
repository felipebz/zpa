-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-PLUGGABLE-DATABASE.html
CREATE PLUGGABLE DATABASE CDB1_PDB1_C AS CLONE USING '/tmp/cdb1_pdb3.pdb'
    KEYSTORE IDENTIFED BY keystore_password DECRYPT USING transport_secret