-- マスタ（IDはBIGINT AUTO_INCREMENT）
CREATE TABLE m_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_name VARCHAR(100) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  standard_price DECIMAL(12,2),
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE m_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_name VARCHAR(100) NOT NULL,
  postal_code VARCHAR(10),
  address VARCHAR(255),
  phone_number VARCHAR(30),
  email VARCHAR(255),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE m_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  menu_name VARCHAR(100) NOT NULL,
  category VARCHAR(50) NOT NULL,
  standard_price DECIMAL(12,2) NOT NULL,
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

-- 消費税率マスタ（税区分ごとの税率を期間で管理する）
CREATE TABLE m_tax_rate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  description VARCHAR(50) NOT NULL,
  rate DECIMAL(5,4) NOT NULL,
  valid_from DATE NOT NULL,
  valid_to DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uk_m_tax_rate_category_valid_from
    UNIQUE (tax_category, valid_from)
);

-- 伝票番号の採番（counter_name = '<purchase_no|sales_no>#<YYYY-MM-DD>'）
CREATE TABLE t_slip_no_counter (
  counter_name VARCHAR(40) PRIMARY KEY,
  value INT NOT NULL
);

-- 仕入（IDはCHAR(36)=UUID。headerを先に定義する）
CREATE TABLE t_purchase_header (
  id CHAR(36) PRIMARY KEY,
  purchase_no VARCHAR(20) NOT NULL UNIQUE,
  supplier_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  delivery_date DATE,
  subtotal DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  remarks VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_purchase_header_supplier
    FOREIGN KEY (supplier_id) REFERENCES m_supplier(id)
);

CREATE INDEX idx_t_purchase_header_purchase_date
  ON t_purchase_header (purchase_date);

CREATE TABLE t_purchase_detail (
  id CHAR(36) PRIMARY KEY,
  purchase_id CHAR(36) NOT NULL,
  item_id BIGINT NOT NULL,
  quantity DECIMAL(10,2) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  tax_rate DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_purchase_detail_purchase
    FOREIGN KEY (purchase_id) REFERENCES t_purchase_header(id),
  CONSTRAINT fk_t_purchase_detail_item
    FOREIGN KEY (item_id) REFERENCES m_item(id)
);

-- 売上（IDはCHAR(36)=UUID。headerを先に定義する）
CREATE TABLE t_sales_header (
  id CHAR(36) PRIMARY KEY,
  sales_no VARCHAR(20) NOT NULL UNIQUE,
  sales_datetime DATETIME NOT NULL,
  business_date DATE NOT NULL,
  table_no VARCHAR(10),
  customer_count INT NOT NULL DEFAULT 1,
  subtotal DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  total_amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(20) NOT NULL,
  remarks VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_sales_header_business_date
  ON t_sales_header (business_date);

CREATE TABLE t_sales_detail (
  id CHAR(36) PRIMARY KEY,
  sales_id CHAR(36) NOT NULL,
  menu_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  tax_rate DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_sales_detail_sales
    FOREIGN KEY (sales_id) REFERENCES t_sales_header(id),
  CONSTRAINT fk_t_sales_detail_menu
    FOREIGN KEY (menu_id) REFERENCES m_menu(id)
);
