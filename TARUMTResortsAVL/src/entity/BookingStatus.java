package entity;

/**
 * BookingStatus.java
 * Entity 类 —— 一笔订房请求(Booking)的状态枚举
 *
 * @author 某某
 *
 * 状态流程(由Control层驱动改变):
 *   PENDING     — 已登记/已排队,还没分到房
 *   CONFIRMED   — 已分配到房间,等待客人入住
 *   CHECKED_IN  — 客人已入住
 *   CHECKED_OUT — 客人已退房
 *   CANCELLED   — 这笔订房请求被取消
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
