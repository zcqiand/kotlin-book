/** 列表查询：恒拼上下文租户谓词，只回边界内成员行。 */
suspend fun listMembersInContext(tenantId: Uuid): List<MemberRow> =
    suspendTransaction(db) {
      TenantMemberTable.selectAll()
          .where { TenantMemberTable.tenantId eq tenantId }
          .orderBy(
              TenantMemberTable.createdAt to SortOrder.ASC,
              TenantMemberTable.id to SortOrder.ASC,
          )
          .map { row ->
            MemberRow(
                id = row[TenantMemberTable.id],
                userId = row[TenantMemberTable.userId],
                tenantId = row[TenantMemberTable.tenantId],
                memberName = row[TenantMemberTable.memberName],
                status = row[TenantMemberTable.status].toInt(),
            )
          }
          .toList()
    }

/** 主键读：主键条件与租户条件同时成立才命中，越界读返回 null。 */
suspend fun readMemberInContext(tenantId: Uuid, memberId: Uuid): MemberRow? =
    suspendTransaction(db) {
      TenantMemberTable.selectAll()
          .where {
            (TenantMemberTable.id eq memberId) and
                (TenantMemberTable.tenantId eq tenantId)
          }
          .firstOrNull()
          ?.let { row ->
            MemberRow(
                id = row[TenantMemberTable.id],
                userId = row[TenantMemberTable.userId],
                tenantId = row[TenantMemberTable.tenantId],
                memberName = row[TenantMemberTable.memberName],
                status = row[TenantMemberTable.status].toInt(),
            )
          }
    }