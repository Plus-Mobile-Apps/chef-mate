@file:OptIn(ExperimentalForeignApi::class)

package com.plusmobileapps.chefmate.redesign

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Hands native bytes (Swift `Data`) to BLoCs that take a Kotlin [ByteArray], e.g. a photo picked in
 * the SwiftUI AI chat. One `memcpy`, instead of setting a `KotlinByteArray` element by element from
 * Swift.
 */
object NativeBytes {
    fun fromData(data: NSData): ByteArray {
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { array ->
            array.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
