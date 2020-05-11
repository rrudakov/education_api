CREATE TABLE articles (
       id SERIAL PRIMARY KEY,
       user_id INTEGER REFERENCES users (id) NOT NULL,
       title VARCHAR(100) NOT NULL,
       body TEXT NOT NULL,
       featured_image VARCHAR(200),
       is_main_featured BOOLEAN NOT NULL DEFAULT FALSE,
       like_counter INTEGER NOT NULL DEFAULT 0,
       created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
       updated_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
