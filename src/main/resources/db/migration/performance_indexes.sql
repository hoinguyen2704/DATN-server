-- Performance indexes for the large demo/production-like datasets.
-- This mirrors scraper/lib/demo-indexes.js, but keeps the indexes in SQL migration form.
-- It intentionally uses normal CREATE INDEX because Flyway-style migrations run in a transaction.
-- For zero-downtime production rollout on very large tables, create these indexes CONCURRENTLY
-- in an operational window instead of running this file inside a transaction.

CREATE OR REPLACE FUNCTION pg_temp.create_index_if_table_exists(table_name text, index_sql text)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    IF to_regclass(format('public.%I', table_name)) IS NOT NULL THEN
        EXECUTE index_sql;
    END IF;
END;
$$;

SELECT pg_temp.create_index_if_table_exists(
    'export_jobs',
    'CREATE INDEX IF NOT EXISTS idx_export_jobs_status ON public.export_jobs USING btree (status)'
);

SELECT pg_temp.create_index_if_table_exists(
    'export_jobs',
    'CREATE INDEX IF NOT EXISTS idx_export_jobs_expires_at ON public.export_jobs USING btree (expires_at)'
);

SELECT pg_temp.create_index_if_table_exists(
    'addresses',
    'CREATE INDEX IF NOT EXISTS idx_perf_addr_user ON public.addresses USING btree (user_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'users',
    'CREATE INDEX IF NOT EXISTS idx_perf_users_created_id ON public.users USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'users',
    'CREATE INDEX IF NOT EXISTS idx_perf_users_role_created_id ON public.users USING btree (role_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'users',
    'CREATE INDEX IF NOT EXISTS idx_perf_users_status_created_id ON public.users USING btree (status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'order_items',
    'CREATE INDEX IF NOT EXISTS idx_demo_order_items_order_id ON public.order_items USING btree (order_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'order_items',
    'CREATE INDEX IF NOT EXISTS idx_perf_order_items_variant_id ON public.order_items USING btree (variant_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_demo_orders_created_at_id_desc ON public.orders USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_demo_orders_status_created_at_id_desc ON public.orders USING btree (order_status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_perf_orders_user_created_id ON public.orders USING btree (user_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_perf_orders_user_status_created_id ON public.orders USING btree (user_id, order_status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_perf_orders_pay_status_created_id ON public.orders USING btree (payment_status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_perf_orders_coupon_code_upper ON public.orders USING btree (UPPER(coupon_code)) WHERE coupon_code IS NOT NULL'
);

SELECT pg_temp.create_index_if_table_exists(
    'orders',
    'CREATE INDEX IF NOT EXISTS idx_perf_orders_ship_coupon_code_upper ON public.orders USING btree (UPPER(shipping_coupon_code)) WHERE shipping_coupon_code IS NOT NULL'
);

SELECT pg_temp.create_index_if_table_exists(
    'order_status_history',
    'CREATE INDEX IF NOT EXISTS idx_perf_order_hist_order_created_id ON public.order_status_history USING btree (order_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'notifications',
    'CREATE INDEX IF NOT EXISTS idx_perf_notif_user_created_id ON public.notifications USING btree (user_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'notifications',
    'CREATE INDEX IF NOT EXISTS idx_perf_notif_user_read_created_id ON public.notifications USING btree (user_id, is_read, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'admin_notifications',
    'CREATE INDEX IF NOT EXISTS idx_perf_admin_notif_created_id ON public.admin_notifications USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'feedbacks',
    'CREATE INDEX IF NOT EXISTS idx_perf_fb_created_id ON public.feedbacks USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'feedbacks',
    'CREATE INDEX IF NOT EXISTS idx_perf_fb_status_created_id ON public.feedbacks USING btree (status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'feedbacks',
    'CREATE INDEX IF NOT EXISTS idx_perf_fb_product_status_created_id ON public.feedbacks USING btree (product_id, status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'feedbacks',
    'CREATE INDEX IF NOT EXISTS idx_perf_fb_user_product_order_variant_created ON public.feedbacks USING btree (user_id, product_id, order_id, variant_id, created_at ASC, id ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'products',
    'CREATE INDEX IF NOT EXISTS idx_perf_products_created_id ON public.products USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'products',
    'CREATE INDEX IF NOT EXISTS idx_perf_products_status_created_id ON public.products USING btree (status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'products',
    'CREATE INDEX IF NOT EXISTS idx_perf_products_category_created_id ON public.products USING btree (category_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'products',
    'CREATE INDEX IF NOT EXISTS idx_perf_products_brand_created_id ON public.products USING btree (brand_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'product_variants',
    'CREATE INDEX IF NOT EXISTS idx_perf_product_variants_product_stock ON public.product_variants USING btree (product_id, stock_quantity)'
);

SELECT pg_temp.create_index_if_table_exists(
    'product_variants',
    'CREATE INDEX IF NOT EXISTS idx_perf_product_variants_product_active_price ON public.product_variants USING btree (product_id, status, price)'
);

SELECT pg_temp.create_index_if_table_exists(
    'brands',
    'CREATE INDEX IF NOT EXISTS idx_perf_brands_created_id ON public.brands USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'categories',
    'CREATE INDEX IF NOT EXISTS idx_perf_categories_created_id ON public.categories USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'category_spec_attributes',
    'CREATE INDEX IF NOT EXISTS idx_perf_category_spec_attrs_category_sort ON public.category_spec_attributes USING btree (category_id, sort_order ASC, id ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'category_variant_attributes',
    'CREATE INDEX IF NOT EXISTS idx_perf_category_variant_attrs_category_sort ON public.category_variant_attributes USING btree (category_id, sort_order ASC, id ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'tickets',
    'CREATE INDEX IF NOT EXISTS idx_perf_tickets_created_id ON public.tickets USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'tickets',
    'CREATE INDEX IF NOT EXISTS idx_perf_tickets_status_created_id ON public.tickets USING btree (status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'tickets',
    'CREATE INDEX IF NOT EXISTS idx_perf_tickets_user_created_id ON public.tickets USING btree (user_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'ticket_messages',
    'CREATE INDEX IF NOT EXISTS idx_perf_ticket_msgs_ticket_created_id ON public.ticket_messages USING btree (ticket_id, created_at ASC, id ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'return_requests',
    'CREATE INDEX IF NOT EXISTS idx_perf_returns_created_id ON public.return_requests USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'return_requests',
    'CREATE INDEX IF NOT EXISTS idx_perf_returns_status_created_id ON public.return_requests USING btree (status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'return_requests',
    'CREATE INDEX IF NOT EXISTS idx_perf_returns_user_created_id ON public.return_requests USING btree (user_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'return_requests',
    'CREATE INDEX IF NOT EXISTS idx_perf_returns_user_status_created_id ON public.return_requests USING btree (user_id, status, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'return_status_history',
    'CREATE INDEX IF NOT EXISTS idx_perf_return_hist_req_created_id ON public.return_status_history USING btree (return_request_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'refund_transactions',
    'CREATE INDEX IF NOT EXISTS idx_perf_refunds_return_created_id ON public.refund_transactions USING btree (return_request_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'flash_sales',
    'CREATE INDEX IF NOT EXISTS idx_perf_flash_sales_created_id ON public.flash_sales USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'flash_sales',
    'CREATE INDEX IF NOT EXISTS idx_perf_flash_sales_status_time ON public.flash_sales USING btree (status, start_time ASC, end_time ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'flash_sale_items',
    'CREATE INDEX IF NOT EXISTS idx_perf_flash_sale_items_variant_id ON public.flash_sale_items USING btree (variant_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'flash_sale_items',
    'CREATE INDEX IF NOT EXISTS idx_perf_flash_sale_items_flash_sale_id ON public.flash_sale_items USING btree (flash_sale_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'coupons',
    'CREATE INDEX IF NOT EXISTS idx_perf_coupons_created_id ON public.coupons USING btree (created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'coupons',
    'CREATE INDEX IF NOT EXISTS idx_perf_coupons_public_status_end ON public.coupons USING btree (is_public, status, end_date ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'coupons',
    'CREATE INDEX IF NOT EXISTS idx_perf_coupons_status_end ON public.coupons USING btree (status, end_date ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'coupon_applicable_products',
    'CREATE INDEX IF NOT EXISTS idx_perf_coupon_applicable_products_coupon ON public.coupon_applicable_products USING btree (coupon_id, product_id)'
);

SELECT pg_temp.create_index_if_table_exists(
    'wishlists',
    'CREATE INDEX IF NOT EXISTS idx_perf_wishlists_user_created_id ON public.wishlists USING btree (user_id, created_at DESC, id DESC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'product_images',
    'CREATE INDEX IF NOT EXISTS idx_perf_product_images_product_sort ON public.product_images USING btree (product_id, sort_order ASC, id ASC)'
);

SELECT pg_temp.create_index_if_table_exists(
    'product_images',
    'CREATE INDEX IF NOT EXISTS idx_perf_product_images_variant_sort ON public.product_images USING btree (variant_id, sort_order ASC, id ASC)'
);

DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'export_jobs',
        'addresses',
        'users',
        'orders',
        'order_items',
        'order_status_history',
        'notifications',
        'admin_notifications',
        'feedbacks',
        'products',
        'product_variants',
        'brands',
        'categories',
        'category_spec_attributes',
        'category_variant_attributes',
        'tickets',
        'ticket_messages',
        'return_requests',
        'return_status_history',
        'refund_transactions',
        'coupons',
        'coupon_applicable_products',
        'flash_sales',
        'flash_sale_items',
        'wishlists',
        'product_images'
    ]
    LOOP
        IF to_regclass(format('public.%I', table_name)) IS NOT NULL THEN
            EXECUTE format('ANALYZE public.%I', table_name);
        END IF;
    END LOOP;
END;
$$;
