/** 创建租户：status=1（active）入库，返回主键。 */
suspend fun createTenant(tenantKey: String, name: String): Uuid {
  val id = Uuid.random()
  suspendTransaction(db) {
    TenantTable.insert {
      it[TenantTable.id] = id
      it[TenantTable.tenantKey] = tenantKey
      it[TenantTable.name] = name
      it[status] = TENANT_STATUS_ACTIVE
      it[createdAt] = OffsetDateTime.now()
      it[updatedAt] = OffsetDateTime.now()
    }
  }
  return id
}

/** 停用：status 1 改 0，返回受影响行数（0 表示主键不存在）。 */
suspend fun disableTenant(tenantId: Uuid): Int =
    suspendTransaction(db) {
      TenantTable.update({ TenantTable.id eq tenantId }) {
        it[status] = TENANT_STATUS_DISABLED
        it[updatedAt] = OffsetDateTime.now()
      }
    }

/** 删除：先清成员再删租户本体，返回两步各自行数。 */
suspend fun deleteTenant(tenantId: Uuid): Pair<Int, Int> =
    suspendTransaction(db) {
      val members =
          TenantMemberTable.deleteWhere { TenantMemberTable.tenantId eq tenantId }
      val tenants = TenantTable.deleteWhere { TenantTable.id eq tenantId }
      members to tenants
    }