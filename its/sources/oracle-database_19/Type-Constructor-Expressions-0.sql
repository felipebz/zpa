CREATE TYPE address_book_t AS TABLE OF cust_address_typ; 
DECLARE 
   myaddr cust_address_typ := cust_address_typ( 
     '500 Oracle Parkway', 94065, 'Redwood Shores', 'CA','USA'); 
   alladdr address_book_t := address_book_t(); 
BEGIN 
   INSERT INTO customers VALUES ( 
      666999, 'Joe', 'Smith', myaddr, NULL, NULL, NULL, NULL, 
      NULL, NULL, NULL, NULL, NULL, NULL, NULL); 
END; 
/