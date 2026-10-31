package demo

// 反例：infix 只收单参数，双参数直接编译失败
infix fun MessageBatch.mergeAll(
    other: MessageBatch,
    extra: MessageBatch,
): MessageBatch = MessageBatch(items + other.items)
