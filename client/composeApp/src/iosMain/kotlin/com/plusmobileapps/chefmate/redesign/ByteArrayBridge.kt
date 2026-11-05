package com.plusmobileapps.chefmate.redesign

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Hands bytes Swift loaded (e.g. a photo from `PhotosPicker`) to BLoC APIs that take a Kotlin
 * [ByteArray], such as `RecipeListBloc.onScanRecipePhotoPicked`.
 */
object ByteArrayBridge {
    @OptIn(ExperimentalForeignApi::class)
    fun fromData(data: NSData): ByteArray {
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { array ->
            array.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
