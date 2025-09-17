package com.example.pddmate

import java.util.Date

data class ProjectSlot(
    val projectId: Int,
    val courseCode: String,
    val title: String,
    val type: String,
    val capacity: Int,
    val startDate: Date?,
    val endDate: Date?,
    val developerName: String
)