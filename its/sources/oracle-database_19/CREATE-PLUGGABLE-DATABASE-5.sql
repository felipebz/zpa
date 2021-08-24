CREATE PLUGGABLE DATABASE cdb1_pdb4
    ADMIN USER IDENTIFIED BY manager
    FILE_NAME_CONVERT=('cdb1_pdb0, cdb1_pdb4')
    CONTAINER_MAP UPDATE (SPLIT PARTITION cdb1_pdb3 
                            AT (50) 
                            INTO
                            (PARTITION cdb1_pdb3, PARTITION cdb1_pdb3)
    ALTER PLUGGABLE DATABASE cdb1_pdb4 OPEN