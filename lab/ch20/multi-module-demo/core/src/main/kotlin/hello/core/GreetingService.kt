package hello.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * 问候业务: ch2 单体工程 App.kt 的三行输出, 在多模块版里拆成业务逻辑(:core)
 * 与展示(:app)两端。业务规则住在这里, 入口模块只管调用与打印。
 */
class GreetingService {
    private val logger = LoggerFactory.getLogger(GreetingService::class.java)

    /**
     * 挂起函数模拟一次有耗时的问候生成: 业务里常见「先查再算再回」, 这里用
     * delay 占位同样的异步形态, 顺便让 coroutines 依赖真实参战而非挂名。
     */
    suspend fun greet(target: String): String {
        delay(300)
        logger.info("greet() 完成, 目标 = {}", target)
        return "hello-multi: $target 的多模块工程已就绪"
    }

    // 阻塞门面: 协程细节封在 :core 内部, :app 无需感知 suspend 与 runBlocking。
    // 这样 coroutines 依赖可以安心留在 implementation, 下游零传染。
    fun greetBlocking(target: String): String = runBlocking { greet(target) }

    // 横幅生成: 反例 B 实测时这里曾标 internal, :app 跨编译单元直接调用
    // 被编译期拦下; 因 :app 确有正当调用需求, 最终态改回 public
    // （internal 版编译失败回执见 receipts.md）。
    fun buildBanner(tool: String, version: String): String {
        return "[$tool] 版本钉子 = $version (由 :core 生成)"
    }
}
