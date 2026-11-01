/** 换租户：租户存在 + 本人是该租户非停用成员，两道门槛都过才换发。 */
suspend fun switchTenant(userId: Uuid, targetTenantId: Uuid): SwitchTenantResponse {
  val tenantExists =
      suspendTransaction(db) {
        TenantTable.selectAll().where { TenantTable.id eq targetTenantId }.firstOrNull() != null
      }
  if (!tenantExists) {
    throw TenantSwitchException(TenantSwitchException.Kind.NOT_FOUND, "tenant $targetTenantId")
  }
  val isMember =
      suspendTransaction(db) {
        TenantMemberTable.selectAll()
            .where {
              (TenantMemberTable.userId eq userId) and
                  (TenantMemberTable.tenantId eq targetTenantId)
            }
            .any { it[TenantMemberTable.status] != 0.toShort() }
      }
  if (!isMember) {
    throw TenantSwitchException(
        TenantSwitchException.Kind.NOT_FOUND,
        "user $userId is not an active member of tenant $targetTenantId",
    )
  }
  return SwitchTenantResponse(
      accessToken = "lab-at-$userId-$targetTenantId",
      refreshToken = "saas-rt-$userId-${System.currentTimeMillis()}-$targetTenantId",
      expiresAt = OffsetDateTime.now().plusSeconds(3600).toString(),
      tenantId = targetTenantId,
  )
}