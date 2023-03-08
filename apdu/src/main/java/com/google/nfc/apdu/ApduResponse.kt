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

package com.google.nfc.apdu

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Objects

/**
 * An APDU Response. SW bytes are not validated.
 *
 * Responses can be deserialized from bytes with [ApduResponse.parse] or serialized with
 * [serialize].
 */
class ApduResponse(
  /**
   * SW1 status byte.
   */
  val sw1: Byte,

  /**
   * SW2 status byte.
   */
  val sw2: Byte,

  /**
   * Response data.
   */
  val data: ByteArray = byteArrayOf(),
) {
  /**
   * Serialize the data object to a byte array, per ISO-7816-4.
   */
  fun serialize(): ByteArray {
    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)

    if (data.isNotEmpty()) {
      output.write(data)
    }

    output.write(byteArrayOf(sw1, sw2))

    return baos.toByteArray()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ApduResponse) return false

    if (sw1 != other.sw1) return false
    if (sw2 != other.sw2) return false
    if (!data.contentEquals(other.data)) return false

    return true
  }

  /**
   * Returns true if the status word is considered successful (if it's `0x9000`).
   */
  val isSuccessful: Boolean
    get() = (sw1 == 0x90.toByte() && sw2 == 0x00.toByte())

  override fun hashCode() = Objects.hash(sw1, sw2, data.contentHashCode())

  override fun toString() =
    "ApduResponse(sw1=0x${sw1.toHexString()}, sw2=0x${sw2.toHexString()}, data=[${data.toHexString()}])"

  companion object {
    /**
     * Parse the given [byteArray] into a [ApduResponse] object.
     */
    @JvmStatic
    fun parse(byteArray: ByteArray): ApduResponse {
      require(byteArray.size >= 2) {
        "Expecting 2 or more bytes."
      }

      return ApduResponse(
        sw1 = byteArray[byteArray.size - 2],
        sw2 = byteArray[byteArray.size - 1],
        data = byteArray.slice(0 until byteArray.size - 2).toByteArray()
      )
    }
  }
}
