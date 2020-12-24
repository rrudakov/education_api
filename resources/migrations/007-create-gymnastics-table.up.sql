CREATE TABLE gymnastics (
  id SERIAL PRIMARY KEY,
  subtype_id INTEGER NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT NOT NULL,
  picture VARCHAR(1000),
  created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);