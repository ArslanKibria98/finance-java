-- V13: Add country_id FK to products table to link a product to a country.

ALTER TABLE products ADD COLUMN country_id UUID;

ALTER TABLE products
    ADD CONSTRAINT products_country_id_fkey
    FOREIGN KEY (country_id) REFERENCES countries(id);

CREATE INDEX idx_products_country ON products(country_id);
