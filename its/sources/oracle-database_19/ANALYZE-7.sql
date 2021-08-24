SELECT owner_name, table_name, head_rowid, analyze_timestamp 
    FROM chained_rows
    ORDER BY owner_name, table_name, head_rowid, analyze_timestamp; 