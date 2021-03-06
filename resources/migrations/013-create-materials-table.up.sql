CREATE TABLE materials (
  id SERIAL PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  description TEXT NOT NULL,
  preview VARCHAR(1000) NOT NULL,
  store_link VARCHAR(1000) NOT NULL,
  price NUMERIC(18, 4),
  created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
