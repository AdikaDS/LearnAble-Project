package com.adika.learnable.model

import com.google.gson.annotations.SerializedName

data class EmailJSRequest(
    @SerializedName("service_id")
    val serviceId: String,
    @SerializedName("template_id")
    val templateId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("template_params")
    val templateParams: Map<String, String>
)

data class EmailJSResponse(
    val status: String,
    val text: String? = null
)
