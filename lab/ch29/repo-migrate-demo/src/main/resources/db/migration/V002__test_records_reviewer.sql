-- V002：检测记录加复核人列（结构演进的增量一步）
ALTER TABLE "test_records" ADD COLUMN IF NOT EXISTS "reviewer" text;
