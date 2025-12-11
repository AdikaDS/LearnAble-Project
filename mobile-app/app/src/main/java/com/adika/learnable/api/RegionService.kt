package com.adika.learnable.api

import com.adika.learnable.model.ProvinceDto
import com.adika.learnable.model.RegencyDto
import retrofit2.http.GET
import retrofit2.http.Path

interface RegionService {
    @GET("api/provinces.json")
    suspend fun getProvinces(): List<ProvinceDto>

    @GET("api/regencies/{provinceId}.json")
    suspend fun getRegencies(
        @Path("provinceId") provinceId: String
    ): List<RegencyDto>
}