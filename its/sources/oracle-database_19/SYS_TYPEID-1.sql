SELECT b.title, b.author.name, SYS_TYPEID(author)
   "Type_ID" FROM books b;