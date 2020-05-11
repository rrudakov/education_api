ALTER TABLE user_roles ADD CONSTRAINT user_id_role_id_key UNIQUE (user_id, role_id);
