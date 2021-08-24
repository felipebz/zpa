SELECT /*+ NO_INDEX_FFS(items item_order_ix) */ order_id
  FROM order_items items;