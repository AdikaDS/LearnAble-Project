package com.adika.learnable.model

data class ProvinceDto(
    val id: String,
    val name: String
)

data class RegencyDto(
    val id: String,
    val province_id: String,
    val name: String
)