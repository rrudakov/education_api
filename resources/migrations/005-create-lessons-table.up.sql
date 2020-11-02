CREATE TABLE lessons (
  id SERIAL PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  subtitle VARCHAR(500) NOT NULL,
  description TEXT NOT NULL,
  screenshots VARCHAR(1000) ARRAY,
  price NUMERIC(18, 4) NOT NULL,
  created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
