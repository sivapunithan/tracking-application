package com.spcode.tnpscprep.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors assets/syllabus.json. */
@Serializable
data class SyllabusDto(
    val version: Int,
    @SerialName("_notice") val notice: String = "",
    val subjects: List<SubjectDto>,
)

@Serializable data class SubjectDto(val id: String, val nameEn: String, val nameTa: String, val units: List<UnitDto>)
@Serializable data class UnitDto(val id: String, val nameEn: String, val nameTa: String, val topics: List<TopicDto>)
@Serializable data class TopicDto(val id: String, val nameEn: String, val nameTa: String, val exams: List<String>)
