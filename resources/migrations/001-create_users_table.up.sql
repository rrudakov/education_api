CREATE TABLE users (
       id SERIAL PRIMARY KEY,
       user_name VARCHAR(20) UNIQUE NOT NULL,
       user_password VARCHAR(100) NOT NULL,
       user_email VARCHAR(100) NOT NULL,
       is_admin BOOLEAN NOT NULL,
       created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
       updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
