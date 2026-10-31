package demo

// 全章共用的消息模型：一个开放基类 + 两个子类型，
// 后续泛型上界、型变、reified 演示都在这棵类型树上展开

open class Message(val id: String)

class TextMessage(id: String, val body: String) : Message(id)

class SystemNotice(id: String, val code: Int) : Message(id)
