package com.example.wmtsmap.controller

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException

internal actual fun decodeWmtsTile(bytes: ByteArray): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw WmtsMapException(WmtsMapError.Network("The WMTS response is not a supported raster image."))
    return bitmap.asImageBitmap()
}
