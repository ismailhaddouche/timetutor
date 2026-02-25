package com.haddouche.timetutor.model

data class Lesson(
    val id: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val startTime: String = "", // Formato HH:mm
    val endTime: String = "",   // Formato HH:mm
    val date: String = "",      // Formato yyyy-MM-dd
    val status: String = "no_impartida", // no_impartida, impartida, ausencia
    val categoryId: String = "",
    val categoryName: String = "",
    val recurrenceType: String = "none", // none, daily, weekly, biweekly, monthly
    val recurrenceEndDate: String? = null,
    val recurrenceDays: List<Int> = emptyList(), // 0=Lunes, ..., 6=Domingo (segun Calendar.DAY_OF_WEEK ajuste)
    val isBilled: Boolean = false,
    val invoiceId: String? = null,
    val color: String = "#FFFFFF" // Color hexadecimal para la clase
) {
    // Helper para mostrar horario
    val timeSlot: String
        get() = "$startTime - $endTime"
}
