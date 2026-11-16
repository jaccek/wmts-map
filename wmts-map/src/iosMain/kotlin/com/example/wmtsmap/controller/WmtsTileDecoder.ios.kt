package com.example.wmtsmap.controller

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.example.wmtsmap.model.WmtsMapError
import com.example.wmtsmap.model.WmtsMapException
import org.jetbrains.skia.Image

internal actual fun decodeWmtsTile(bytes: ByteArray): ImageBitmap =
    try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (exception: IllegalArgumentException) {
        throw WmtsMapException(
            WmtsMapError.Network("The WMTS response is not a supported raster image."),
            exception,
        )
    }
