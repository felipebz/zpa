-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-json-relational-duality-view.html
CREATE OR REPLACE JSON RELATIONAL DUALITY VIEW ORDERS_OV AS
SELECT JSON { 'OrderId'   : ord.order_id, 
              'OrderTime' : ord.order_datetime,
	        'OrderStatus' : ord.order_status,
              'CustomerInfo' : 
			  (SELECT JSON{'CustomerId'    : cust.customer_id,
	    	                      'CustomerName'  : cust.full_name,
                                  'CustomerEmail' : cust.email_address }	                                                                         
			   FROM CUSTOMERS cust 
                     WHERE cust.customer_id = ord.customer_id),	
              'OrderItems' : (SELECT JSON_ARRAYAGG(
					JSON { 'OrderItemId' : oi.line_item_id,
                                       'Quantity'    : oi.quantity, 					  			
                                       'ProductInfo' : <subquery from product>        
                                	 'ShipmentInfo' : <subquery from shipments>)                                                                                           
                            }) 
                            FROM ORDER_ITEMS oi		
                            WHERE ord.order_id = oi.order_id) 
}
FROM ORDERS ord;