SELECT family_doc FROM families
  where json_textcontains(family_doc, '$.family.id', '10');