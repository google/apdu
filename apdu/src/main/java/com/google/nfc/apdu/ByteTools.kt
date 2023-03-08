/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("ByteTools")

package com.google.nfc.apdu

/**
 * Converts two Bytes to a single 16-bit Int treating the Bytes as unsigned.
 * e.g. (0x00, 0x00) = 0, (0x00, 0xff) = 255, (0xff, 0xff) = 65535
 */
fun bytesToInt(byteH: Byte, byteL: Byte): Int =
  (byteH.asUnsignedInt().shl(Byte.SIZE_BITS) or byteL.asUnsignedInt())

/**
 * Treats the Byte as an unsigned value, converting it into a signed Integer.
 * e.g. 0x00 = 0, 0xff = 255
 */
internal fun Byte.asUnsignedInt() = this.toUByte().toInt()

/**
 * Outputs the Byte as an uppercase unsigned hex string. (e.g. `"F5"`)
 */
internal fun Byte.toHexString() = String.format("%02X", this)

/**
 * Outputs the [ByteArray] as an uppercase hex string. (e.g. "42CAFE")
 */
internal fun ByteArray.toHexString() = this.joinToString(separator = "") { b -> b.toHexString() }
