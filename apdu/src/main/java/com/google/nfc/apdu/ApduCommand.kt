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
 * An APDU Command as defined in ISO-7816-4. This supports extended length fields.
 */
class ApduCommand(
  /**
   * The APDU command class (CLS). This parameter is not validated.
   */
  val commandClass: Byte,
  /**
   * The APDU instruction (INS).
   */
  val instruction: Byte,

  /**
   * Parameter 1 (P1).
   */
  val parameter1: Byte,

  /**
   * Parameter 2 (P2).
   */
  val parameter2: Byte,

  /**
   * Command data. This can be up to 256 bytes for standard payloads or 65536 bytes for extended.
   */
  val data: ByteArray,

  /**
   * The maximum number of bytes expected in the response data field (Ne/Le).
   */
  val maxExpectedResponseLength: Int,
) {

  init {
    require(maxExpectedResponseLength >= 0) { "maxExpectedResponseLength must not be negative" }
  }

  /**
   * Serialize the object to a byte array per ISO-7816-4. If the data length or expected response
   * size is greater than 256 bytes, extended lengths will be used. Extended lengths can also be
   * enabled by calling [serialize] with [forceExtended] set to `true`.
   */
  @JvmOverloads
  fun serialize(forceExtended: Boolean = false): ByteArray {
    val extended = forceExtended || data.size > NORMAL_LC_MAX ||
      maxExpectedResponseLength > NORMAL_LE_MAX

    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)
    output.write(byteArrayOf(commandClass, instruction, parameter1, parameter2))

    if (data.isNotEmpty()) {
      if (extended) {
        output.writeByte(0x00)
        output.writeShort(data.size)
      } else {
        output.writeByte(data.size)
      }

      output.write(data)
    }

    if (maxExpectedResponseLength > 0) {
      if (extended) {
        if (data.isEmpty()) {
          output.writeByte(0x00)
        }

        if (maxExpectedResponseLength == EXTENDED_LE_MAX) {
          output.writeShort(0x00)
        } else {
          output.writeShort(maxExpectedResponseLength)
        }
      } else {
        if (maxExpectedResponseLength == NORMAL_LE_MAX) {
          output.writeByte(0x00)
        } else {
          output.writeByte(maxExpectedResponseLength)
        }
      }
    }

    return baos.toByteArray()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ApduCommand) return false

    if (commandClass != other.commandClass) return false
    if (instruction != other.instruction) return false
    if (parameter1 != other.parameter1) return false
    if (parameter2 != other.parameter2) return false
    if (!data.contentEquals(other.data)) return false
    if (maxExpectedResponseLength != other.maxExpectedResponseLength) return false

    return true
  }

  override fun hashCode() = Objects.hash(
    commandClass,
    instruction,
    parameter1,
    parameter2,
    data.contentHashCode(),
    maxExpectedResponseLength
  )

  override fun toString() =
    "ApduCommand(commandClass=0x${commandClass.toHexString()}, " +
      "instruction=0x${instruction.toHexString()}, " +
      "parameter1=0x${parameter1.toHexString()}, parameter2=0x${parameter2.toHexString()}, " +
      "data=[${data.toHexString()}], " +
      "maxExpectedResponseLength=$maxExpectedResponseLength)"

  companion object {
    private const val FIELD_COMMAND_CLASS = 0
    private const val FIELD_INSTRUCTION = 1
    private const val FIELD_P1 = 2
    private const val FIELD_P2 = 3
    private const val FIELD_LC = 4
    private const val FIELD_LC_EXTENDED_H = 5
    private const val FIELD_LC_EXTENDED_L = 6
    private const val FIELD_NO_LC_EXTENDED_FLAG = 4

    private const val COMMAND_SIZE_NO_LC_NO_LE = 4
    private const val COMMAND_SIZE_NO_LC = 5
    private const val COMMAND_SIZE_NO_LC_EXTENDED = 7

    private const val NORMAL_LC_MAX = 0xff
    private const val NORMAL_LE_MAX = 0x100
    private const val EXTENDED_LE_MAX = 0x10000

    /**
     * Parses the [byteArray] into an [ApduCommand] according to the ISO-7816-4 encoding.
     *
     * Throws an [IllegalArgumentException] if the response cannot be parsed.
     */
    @JvmStatic
    fun parse(byteArray: ByteArray): ApduCommand {
      require(byteArray.size >= COMMAND_SIZE_NO_LC_NO_LE) {
        "Not enough bytes for APDU command"
      }

      val commandClass = byteArray[FIELD_COMMAND_CLASS]
      val instruction = byteArray[FIELD_INSTRUCTION]
      val p1 = byteArray[FIELD_P1]
      val p2 = byteArray[FIELD_P2]

      // Special cases for omitted Lc or Le fields. These must be detected based on payload size.
      when (byteArray.size) {
        COMMAND_SIZE_NO_LC_NO_LE -> return ApduCommand(
          commandClass = commandClass,
          instruction = instruction,
          parameter1 = p1,
          parameter2 = p2,
          data = byteArrayOf(),
          maxExpectedResponseLength = 0
        )

        COMMAND_SIZE_NO_LC -> return ApduCommand(
          commandClass = commandClass,
          instruction = instruction,
          parameter1 = p1,
          parameter2 = p2,
          data = byteArrayOf(),
          maxExpectedResponseLength = byteArray[4].asUnsignedInt()
        )

        COMMAND_SIZE_NO_LC_EXTENDED -> if (byteArray[FIELD_NO_LC_EXTENDED_FLAG] == (0x00).toByte()) {
          return ApduCommand(
            commandClass = commandClass,
            instruction = instruction,
            parameter1 = p1,
            parameter2 = p2,
            data = byteArrayOf(),
            maxExpectedResponseLength = bytesToInt(byteArray[5], byteArray[6])
          )
        }
      }

      val extendedLength = byteArray[FIELD_LC].asUnsignedInt() == 0x00

      val dataLength: Int = if (extendedLength) {
        bytesToInt(byteArray[FIELD_LC_EXTENDED_H], byteArray[FIELD_LC_EXTENDED_L])
      } else {
        byteArray[FIELD_LC].asUnsignedInt()
      }

      val fieldData = if (extendedLength) {
        FIELD_LC_EXTENDED_L + 1
      } else {
        FIELD_LC + 1
      }

      require(byteArray.size >= (fieldData + dataLength)) {
        "Payload too small for stated data size"
      }

      val data = byteArray.slice(fieldData until (fieldData + dataLength)).toByteArray()

      val fieldLe = fieldData + dataLength

      val maxResponse = when (val extraBytes = byteArray.size - (fieldLe)) {
        0 -> 0
        1 -> {
          val value = byteArray[fieldLe].asUnsignedInt()
          // Special case from ISO-17816-4 section 5.1
          if (value == 0) NORMAL_LE_MAX else value
        }

        2 -> {
          val value = bytesToInt(byteArray[fieldLe], byteArray[fieldLe + 1])
          // Special case from ISO-17816-4 section 5.1
          if (value == 0) EXTENDED_LE_MAX else value
        }

        else -> throw IllegalArgumentException("Found $extraBytes extra byte(s) found at end of APDU payload")
      }

      return ApduCommand(commandClass, instruction, p1, p2, data, maxResponse)
    }
  }
}
