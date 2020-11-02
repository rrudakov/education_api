CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  user_name VARCHAR(20) UNIQUE NOT NULL,
  user_password VARCHAR(100) NOT NULL,
  user_email VARCHAR(100) NOT NULL,
  created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
--;;
CREATE TABLE roles (
  id SERIAL PRIMARY KEY,
  role_name VARCHAR(20) UNIQUE NOT NULL
);
--;;
CREATE TABLE user_roles (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users (id) NOT NULL,
  role_id INTEGER REFERENCES roles (id) NOT NULL
);
--;;
INSERT INTO roles (role_name) VALUES
       ('admin'),
       ('moderator'),
       ('guest');
