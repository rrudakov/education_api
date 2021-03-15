CREATE TABLE email_subscriptions (
  email VARCHAR(500) PRIMARY KEY,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);
