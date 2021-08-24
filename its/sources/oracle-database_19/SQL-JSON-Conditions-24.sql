SELECT family_doc FROM families
  WHERE JSON_TEXTCONTAINS(family_doc, '$', '10');