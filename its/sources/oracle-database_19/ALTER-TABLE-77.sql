INSERT INTO product_information (product_id, product_name, 
   list_price)
   VALUES (300, 'left-handed mouse', 40.50); 

SELECT product_id, product_name, list_price, min_price 
    FROM product_information
    WHERE product_id = 300; 