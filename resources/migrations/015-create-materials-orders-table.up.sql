CREATE TABLE materials_orders (
  order_session_id VARCHAR(200) REFERENCES orders (session_id) PRIMARY KEY,
  material_id INTEGER REFERENCES materials (id) NOT NULL,
  CONSTRAINT unique_session_material UNIQUE (order_session_id, material_id)
);
