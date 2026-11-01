package saas.identity.infra

/** 换租户失败（教学级异常面，对码案例仓 NotFoundError/BadRequestError 语义）。 */
class TenantSwitchException(val kind: Kind, message: String) : Exception(message) {
  enum class Kind { NOT_FOUND, BAD_REQUEST }
}
