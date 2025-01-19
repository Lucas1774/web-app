CREATE TABLE categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    category_order INT NOT NULL
);
GO

CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    is_rare BIT NOT NULL DEFAULT 0,
    category_id INT NULL
);
GO

CREATE TABLE my_table (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ans VARCHAR(50) NULL,
    text VARCHAR(255) NULL,
    text_mode BIT NOT NULL DEFAULT 0
);
GO

INSERT INTO my_table (ans, text) VALUES (NULL, NULL);
GO

CREATE TABLE shopping (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT unique_user_product UNIQUE (user_id, product_id)
);
GO

CREATE TABLE sudokus (
    id INT IDENTITY(1,1) PRIMARY KEY,
    state VARCHAR(255) NOT NULL
);
GO

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL
);
GO

INSERT INTO users (username, password) VALUES ('admin', 'admin');
INSERT INTO users (username, password) VALUES ('default', 'default');
GO
