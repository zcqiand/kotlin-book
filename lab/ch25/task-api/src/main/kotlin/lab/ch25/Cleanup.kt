package lab.ch25

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/** 验收收尾入口：清掉 tasks 表，验证库 taskapi_ch25_verify 保留盘上。gradle -P 不需要，直接 dropTasksTable 任务触发。 */
fun main() = runBlocking {
  val db = connectVerify()
  suspendTransaction(db) { SchemaUtils.drop(TasksTable) }
  println("tasks 表已清理（验证库 $PG_VERIFY_DB 保留）")
}
