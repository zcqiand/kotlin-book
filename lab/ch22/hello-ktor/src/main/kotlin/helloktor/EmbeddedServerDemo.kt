package helloktor

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

// 直启变体：embeddedServer 把服务嵌进本进程，端口作为参数写死，
// 用于最小起步演示；配置文件与热重载走 application.conf + EngineMain 一路。
fun main() {
    embeddedServer(Netty, port = 8091, module = Application::module).start(wait = true)
}
