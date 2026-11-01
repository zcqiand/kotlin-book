package lab.ch29

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 种子装载：读 resources/db/seed 下的 JSON，经仓储 upsert 灌入。
 * upsert 幂等，种子可重跑；业务字段之外的时间列由仓储落库时补。
 */
class SeedLoader(private val repository: SpecialtyRepository) {

  suspend fun seed() {
    val text = javaClass.getResourceAsStream("/db/seed/inspection_specialties.json")
        ?.bufferedReader(Charsets.UTF_8)?.readText()
        ?: error("种子文件缺失：/db/seed/inspection_specialties.json")
    val rows = Json.parseToJsonElement(text).jsonArray
    for (element in rows) {
      val obj = element.jsonObject
      repository.upsert(Specialty(
          code = obj.getValue("code").jsonPrimitive.content,
          officialNo = obj.getValue("official_no").jsonPrimitive.content,
          name = obj.getValue("name").jsonPrimitive.content,
          isOfficial = obj.getValue("is_official").jsonPrimitive.boolean,
          enabled = obj.getValue("enabled").jsonPrimitive.boolean,
          sortOrder = obj.getValue("sort_order").jsonPrimitive.int,
      ))
    }
    println("种子灌入完成：${rows.size} 行（upsert 幂等，可重跑）")
  }
}
