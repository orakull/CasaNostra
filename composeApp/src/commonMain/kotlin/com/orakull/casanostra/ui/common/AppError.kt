package com.orakull.casanostra.ui.common

data class AppError(
    val userMessage: String,
    val technicalDetail: String,
)

fun Exception.toAppError(context: String? = null): AppError {
    val tech = "${this::class.simpleName ?: "Exception"}: ${message ?: "no message"}"
    val friendly = mapExceptionToFriendly(this)
    return AppError(
        userMessage = if (context != null) "$context. $friendly" else friendly,
        technicalDetail = tech
    )
}

private fun mapExceptionToFriendly(e: Exception): String {
    val msg = (e.message ?: "").lowercase()
    return when {
        "network" in msg || "connectexception" in msg || "unreachable" in msg
                || "no route" in msg || "connection refused" in msg ->
            "Нет подключения к интернету"
        "timeout" in msg || "timedout" in msg || "timed out" in msg ->
            "Превышено время ожидания"
        "401" in msg || "unauthorized" in msg ->
            "Ошибка авторизации. Попробуйте войти заново"
        "403" in msg || "forbidden" in msg ->
            "Нет доступа к ресурсу"
        "404" in msg || "not found" in msg ->
            "Данные не найдены"
        "500" in msg || "502" in msg || "503" in msg || "internal server" in msg ->
            "Ошибка на сервере. Попробуйте позже"
        "jwt" in msg || "invalid token" in msg ->
            "Сессия истекла. Войдите заново"
        else -> "Что-то пошло не так"
    }
}
