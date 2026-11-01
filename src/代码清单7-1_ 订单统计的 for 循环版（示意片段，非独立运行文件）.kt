// 同一件事的两段统计，各养一条循环：累加变量自己起名、
// 筛选条件自己写 if、初值自己置——这就是操作符出场前的日常
var paidCount = 0
for (order in orders) {
    if (order.paid) paidCount++
}

var paidTotal = 0.0
for (order in orders) {
    if (order.paid) paidTotal += order.amount
}