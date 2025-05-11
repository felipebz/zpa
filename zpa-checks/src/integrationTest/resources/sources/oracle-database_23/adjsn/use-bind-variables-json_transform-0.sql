-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/use-bind-variables-json_transform.html
CREATE JSON COLLECTION TABLE customers;
INSERT INTO customers VALUES ('{"_id"    : 1234,
                                "name"   : "Jane Doe",
                                "email"  : "jane@example.com",
                                "tags"   : [ "newsletter" ],
                                "status" : "gold",
                                "joined" : "2024"}');