package com.adika.learnable.model

import com.google.gson.annotations.SerializedName

data class ImgurResponse(

	@field:SerializedName("data")
	val data: Data? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("status")
	val status: Int? = null
)

data class Data(

	@field:SerializedName("link")
	val link: String? = null,
)
