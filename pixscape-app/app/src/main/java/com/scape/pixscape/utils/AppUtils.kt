package com.scape.pixscape.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream


fun downloadKmlFileAsync() = GlobalScope.async {
    val client = OkHttpClient()
    val request = Request.Builder().url("https://scapekit-resources.s3-eu-west-1.amazonaws.com/parking_areas.kml").build()
    val response = client.newCall(request).execute()
    ByteArrayInputStream(response.body?.string()?.toByteArray(Charsets.UTF_8))
}


