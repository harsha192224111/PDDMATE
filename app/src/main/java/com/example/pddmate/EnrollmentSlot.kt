package com.example.pddmate

data class EnrollmentSlot(
    val projectId: Int,
    val courseCode: String,
    val title: String,
    val type: String,
    val capacity: Int,
    val slotsLeft: Int,
    val mentorName: String,
    var status: String // Now mutable to update status easily
)
