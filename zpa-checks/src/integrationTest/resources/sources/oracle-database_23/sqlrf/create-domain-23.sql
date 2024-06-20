-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN email AS VARCHAR2(30)  
    CONSTRAINT EMAIL_C CHECK (REGEXP_LIKE (email, '^(\S+)\@(\S+)\.(\S+)$'))
    DISPLAY '---' || SUBSTR(email, INSTR(email, '@') + 1)
    ANNOTATIONS(allowed_operations 
'{
    "allowed_operations": {
        "title": "Allowed operations",
        "operations": [
            "Sort",
            "Group By",
            "Picklist"
        ]
    }
}');