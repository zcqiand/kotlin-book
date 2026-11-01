package demo

// 想重载加号，函数名却起了约定表外的 add
operator fun MessageBatch.add(other: MessageBatch): MessageBatch =
    MessageBatch(items + other.items)