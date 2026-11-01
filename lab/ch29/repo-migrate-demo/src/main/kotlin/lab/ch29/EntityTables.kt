package lab.ch29

import org.jetbrains.exposed.v1.core.Table

/**
 * 两张教学表对象，列形状逐列对照案例仓 io/xr/lab/entity 同名表对象
 * （DB-First 反推生成物，ADR-0046 同口径）。收样单等 46 列大表不进教学工程，
 * 全景映射见正文 29.2 的表 29-1。
 */
object InspectionSpecialtiesTable : Table("inspection_specialties") {
  val code = text("code")
  val officialNo = text("official_no")
  val name = text("name")
  val isOfficial = bool("is_official")
  val enabled = bool("enabled")
  val sortOrder = integer("sort_order")
  val createdAt = text("created_at")
  val updatedAt = text("updated_at")
  override val primaryKey = PrimaryKey(code)
}

object TestRecordsTable : Table("test_records") {
  val id = text("id")
  val sampleId = text("sample_id")
  val parameterCode = text("parameter_code")
  val standardCode = text("standard_code").nullable()
  val requirementCode = text("requirement_code").nullable()
  val requirement = text("requirement")
  val result = text("result")
  val verdict = text("verdict").nullable()
  val createdAt = text("created_at")
  val updatedAt = text("updated_at")
  val tenantId = text("tenant_id")
  override val primaryKey = PrimaryKey(id)
}
