package com.haddouche.timetutor.model

data class Invoice(
    val id: String = "",
    val teacherUid: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val studentLastName: String = "",
    val studentWhatsapp: String = "",
    val studentEmail: String = "",
    val date: String = "",
    val totalAmount: Double = 0.0,
    val paid: Boolean = false,
    val lessonIds: List<String> = emptyList() // IDs of lessons included in this invoice
)
