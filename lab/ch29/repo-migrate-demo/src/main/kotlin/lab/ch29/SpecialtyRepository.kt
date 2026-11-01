package lab.ch29

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

/** 领域对象：专业字典。只讲业务语义，不带任何行与列的概念。 */
data class Specialty(
    val code: String,
    val officialNo: String,
    val name: String,
    val isOfficial: Boolean,
    val enabled: Boolean,
    val sortOrder: Int,
)

/** 仓储接口：调用方看到的是「专业」这个领域对象与四个业务动作。 */
interface SpecialtyRepository {
  suspend fun findAll(): List<Specialty>
  suspend fun findByCode(code: String): Specialty?
  suspend fun upsert(specialty: Specialty)
  suspend fun setEnabled(code: String, enabled: Boolean)
}

/**
 * Exposed 实现：接口的每个动作各包一个 suspendTransaction，
 * 事务边界=一个业务动作；实现细节（DSL、行映射）不出这个类。
 */
class ExposedSpecialtyRepository(private val db: R2dbcDatabase) : SpecialtyRepository {

  override suspend fun findAll(): List<Specialty> = suspendTransaction(db) {
    InspectionSpecialtiesTable.selectAll()
        .orderBy(InspectionSpecialtiesTable.sortOrder, SortOrder.ASC)
        .map { it.toSpecialty() }
        .toList()
  }

  override suspend fun findByCode(code: String): Specialty? = suspendTransaction(db) {
    InspectionSpecialtiesTable.selectAll()
        .where { InspectionSpecialtiesTable.code eq code }
        .firstOrNull()
        ?.toSpecialty()
  }

  override suspend fun upsert(specialty: Specialty): Unit = suspendTransaction(db) {
    val updated = InspectionSpecialtiesTable.update({ InspectionSpecialtiesTable.code eq specialty.code }) {
      it[officialNo] = specialty.officialNo
      it[name] = specialty.name
      it[isOfficial] = specialty.isOfficial
      it[enabled] = specialty.enabled
      it[sortOrder] = specialty.sortOrder
      it[updatedAt] = now()
    }
    if (updated == 0) {
      InspectionSpecialtiesTable.insert {
        it[code] = specialty.code
        it[officialNo] = specialty.officialNo
        it[name] = specialty.name
        it[isOfficial] = specialty.isOfficial
        it[enabled] = specialty.enabled
        it[sortOrder] = specialty.sortOrder
        it[createdAt] = now()
        it[updatedAt] = now()
      }
    }
  }

  override suspend fun setEnabled(code: String, enabled: Boolean): Unit = suspendTransaction(db) {
    InspectionSpecialtiesTable.update({ InspectionSpecialtiesTable.code eq code }) {
      it[InspectionSpecialtiesTable.enabled] = enabled
      it[updatedAt] = now()
    }
  }

  private fun ResultRow.toSpecialty(): Specialty = Specialty(
      code = this[InspectionSpecialtiesTable.code],
      officialNo = this[InspectionSpecialtiesTable.officialNo],
      name = this[InspectionSpecialtiesTable.name],
      isOfficial = this[InspectionSpecialtiesTable.isOfficial],
      enabled = this[InspectionSpecialtiesTable.enabled],
      sortOrder = this[InspectionSpecialtiesTable.sortOrder],
  )

  /** 落库时间列与案例仓同形：text 秒级时间戳。 */
  private fun now(): String =
      java.time.LocalDateTime.now().toString().take(19).replace("T", " ")
}
