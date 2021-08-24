CREATE TABLE order_detail 
  (CONSTRAINT pk_od PRIMARY KEY (order_id, part_no), 
   order_id    NUMBER 
      CONSTRAINT fk_oid 
         REFERENCES oe.orders(order_id), 
   part_no     NUMBER 
      CONSTRAINT fk_pno 
         REFERENCES oe.product_information(product_id), 
   quantity    NUMBER 
      CONSTRAINT nn_qty NOT NULL 
      CONSTRAINT check_qty CHECK (quantity > 0), 
   cost        NUMBER 
      CONSTRAINT check_cost CHECK (cost > 0) );