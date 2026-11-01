-- V001：初始结构（对照案例仓 drizzle/0000_target_ddl.sql 同表节选形状）
CREATE TABLE IF NOT EXISTS "inspection_specialties" (
    "code" text PRIMARY KEY NOT NULL,
    "official_no" text NOT NULL,
    "name" text NOT NULL,
    "is_official" boolean NOT NULL,
    "enabled" boolean NOT NULL,
    "sort_order" integer NOT NULL,
    "created_at" text DEFAULT '' NOT NULL,
    "updated_at" text DEFAULT '' NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "test_records" (
    "id" text PRIMARY KEY NOT NULL,
    "sample_id" text NOT NULL,
    "parameter_code" text NOT NULL,
    "standard_code" text,
    "requirement_code" text,
    "requirement" text NOT NULL,
    "result" text NOT NULL,
    "verdict" text,
    "created_at" text DEFAULT '' NOT NULL,
    "updated_at" text DEFAULT '' NOT NULL,
    "tenant_id" text DEFAULT '' NOT NULL
);
