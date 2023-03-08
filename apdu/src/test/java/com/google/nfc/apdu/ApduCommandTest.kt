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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class ApduCommandTest {

  @Test
  fun `parse the smallest valid command`() {
    val expectedSerialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(),
      maxExpectedResponseLength = 0,
    )

    val parsed = ApduCommand.parse(expectedSerialized)
    assertEquals(expected, parsed)
    assertEquals(expected.hashCode(), parsed.hashCode())
    assertArrayEquals(expectedSerialized, expected.serialize())
  }

  @Test
  fun `parse a command with data size 1`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x01, BYTE_3)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = 0
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with data size 2`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x02, BYTE_3, BYTE_4)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3, BYTE_4),
      maxExpectedResponseLength = 0
    )

    val parsed = ApduCommand.parse(serialized)
    assertEquals(expected, parsed)
    assertEquals(expected.hashCode(), parsed.hashCode())
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with data size 0 and a nonzero expended response size`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, BYTE_3)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(),
      maxExpectedResponseLength = BYTE_3.asUnsignedInt()
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with an extended length response length and serialize to the same`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, BYTE_3)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(),
      maxExpectedResponseLength = BYTE_3.asUnsignedInt()
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize(forceExtended = true))
  }

  @Test
  fun `parse a command with 1 byte data and a standard size expected response length`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x01, BYTE_3, BYTE_4)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = BYTE_4.asUnsignedInt()
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with extended length data and standard length response size`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, 0x01, BYTE_3)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = 0
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize(forceExtended = true))
  }

  @Test
  fun `parsing and serializing data and expected response length with extended sizes`() {
    val serialized =
      byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, 0x01, BYTE_3, 0x00, BYTE_4)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = BYTE_4.asUnsignedInt()
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize(forceExtended = true))
  }

  @Test
  fun `parse a command with 1 byte data and 256 bytes of expected response size`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x01, BYTE_3, 0x00)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = 256
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with 1 byte of data and the maximum extended response size`() {
    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, 0x01, BYTE_3, 0x00, 0x00)
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = byteArrayOf(BYTE_3),
      maxExpectedResponseLength = 65536
    )

    assertEquals(expected, ApduCommand.parse(serialized))
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with exactly 255 bytes of data`() {
    val data = (0 until 255).map { i -> (i.mod(256)).toByte() }.toByteArray()

    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, b(0xFF)) + data
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = data,
      maxExpectedResponseLength = 0,
    )

    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse a command with more than 256 bytes of data`() {
    val data = (0 until 617).map { i -> (i.mod(256)).toByte() }.toByteArray()

    val serialized = byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00) + intTo2Bytes(data.size) + data
    val expected = ApduCommand(
      commandClass = CLA_1,
      instruction = INS_1,
      parameter1 = BYTE_1,
      parameter2 = BYTE_2,
      data = data,
      maxExpectedResponseLength = 0,
    )

    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `throw an error on parsing when the data are too small`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf())
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1))
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1))
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1, BYTE_1))
    }
  }

  @Test
  fun `throw an error on constructing when an illegal argument is provided`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand(BYTE_1, INS_1, BYTE_2, BYTE_3, byteArrayOf(), -1)
    }
  }

  @Test
  fun `throw an error when the there are not enough data bytes in the payload`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x02, BYTE_3))
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, b(0xff), BYTE_3))
    }
  }

  @Test
  fun `throw an error when there are too many bytes in the payload`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(
        byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x01, BYTE_3, BYTE_4, BYTE_5, BYTE_6)
      )
    }
  }

  @Test
  fun `throw an error when using extended sizes and there are not enough bytes in the payload`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, 0x02, BYTE_3))
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduCommand.parse(byteArrayOf(CLA_1, INS_1, BYTE_1, BYTE_2, 0x00, 0x00, b(0xff), BYTE_3))
    }
  }

  @Test
  fun `verify that the b test helper functions properly`() {
    assertEquals(0, b(0x00).toInt())

    assertEquals(0, b(0xff).countLeadingZeroBits())
    assertEquals(0, b(0xff).countTrailingZeroBits())

    assertEquals(1, b(0x7f).countLeadingZeroBits())
    assertEquals(0, b(0x7f).countTrailingZeroBits())

    assertEquals(4, b(0x0f).countLeadingZeroBits())
    assertEquals(0, b(0x0f).countTrailingZeroBits())

    assertEquals(8, b(0x00).countLeadingZeroBits())
    assertEquals(8, b(0x00).countTrailingZeroBits())
  }

  @Test
  fun `verify that the intTo2Bytes test helper functions properly`() {
    assertArrayEquals(byteArrayOf(0x00, 0x00), intTo2Bytes(0))
    assertArrayEquals(byteArrayOf(0x00, b(0xff)), intTo2Bytes(255))

    assertArrayEquals(byteArrayOf(b(0xff), b(0xff)), intTo2Bytes(65535))
  }

  @Test
  fun `toString outputs an expected string`() {
    assertEquals(
      "ApduCommand(commandClass=0x80, instruction=0xA4, parameter1=0x04, parameter2=0x00, data=[], maxExpectedResponseLength=0)",
      ApduCommand(b(0x80), b(0xA4), 0x04, 0x00, byteArrayOf(), 0).toString()
    )

    assertEquals(
      "ApduCommand(commandClass=0x80, instruction=0xA4, parameter1=0x04, parameter2=0x00, data=[CAFE], maxExpectedResponseLength=256)",
      ApduCommand(b(0x80), b(0xA4), 0x04, 0x00, byteArrayOf(b(0xca), b(0xfe)), 256).toString()
    )
  }

  companion object {
    val CLA_1 = b(11)

    val INS_1 = b(13)

    val BYTE_1 = b(23)
    val BYTE_2 = b(29)
    val BYTE_3 = b(31)
    val BYTE_4 = b(37)
    val BYTE_5 = b(41)
    val BYTE_6 = b(43)
  }
}

internal fun b(intValue: Int): Byte = intValue.toByte()
internal fun intTo2Bytes(value: Int): ByteArray {
  return byteArrayOf(((value and 0xff00).shr(8)).toByte(), (value and 0xff).toByte())
}
