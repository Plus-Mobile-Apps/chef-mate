@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.plusmobileapps.chefmate.redesign

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * Converts between Kotlin [ByteArray] and Foundation [NSData] for the SwiftUI redesign, which reads
 * and writes files with the native `fileImporter` / `fileExporter` (recipe import and export
 * archives). Swift sees a `KotlinByteArray`, which has no cheap bridge to `Data` on its own.
 */
object ByteArrayBridge {
    fun toData(bytes: ByteArray): NSData =
        if (bytes.isEmpty()) NSData()
        else
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }

    fun fromData(data: NSData): ByteArray {
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { array ->
            array.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
