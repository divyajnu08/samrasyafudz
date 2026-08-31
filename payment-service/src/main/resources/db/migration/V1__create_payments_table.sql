CREATE TABLE payments
(
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT         NOT NULL,
    user_id             BIGINT         NOT NULL,
    razorpay_order_id   VARCHAR(100)   NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(100),
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'INR',
    method              VARCHAR(20)    NOT NULL DEFAULT 'upi',
    status              VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_razorpay_order_id ON payments (razorpay_order_id);