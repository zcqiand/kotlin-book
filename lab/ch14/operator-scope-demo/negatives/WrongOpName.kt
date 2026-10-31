package demo

// 反例：operator 只认约定名，add 不在约定表里
operator fun MessageBatch.add(other: MessageBatch): MessageBatch =
    MessageBatch(items + other.items)
