package com.scape.pixscape.utils

import com.scape.pixscape.BuildConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream


fun downloadKmlFileAsync() = GlobalScope.async {
    val client = OkHttpClient()
    val request = Request.Builder().url(BuildConfig.KML_LAYER_URL).build()
    val response = client.newCall(request).execute()
    ByteArrayInputStream(response.body?.string()?.toByteArray(Charsets.UTF_8))
}


