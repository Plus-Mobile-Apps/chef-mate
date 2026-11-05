@file:OptIn(ExperimentalForeignApi::class)

package com.plusmobileapps.chefmate.redesign

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/** Converts between Swift `Data` and Kotlin [ByteArray] for BLoC calls that take raw bytes. */
object NativeBytes {
    fun fromData(data: NSData): ByteArray {
        val size = data.length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
        return bytes
    }

    fun toData(bytes: ByteArray): NSData =
        if (bytes.isEmpty()) NSData()
        else
            bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
}
