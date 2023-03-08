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

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApduResponseTest {
  @Test
  fun `parse and serialize with no response data`() {
    val serialized = byteArrayOf(BYTE_1, BYTE_2)
    val expected = ApduResponse(BYTE_1, BYTE_2)

    val actual = ApduResponse.parse(serialized)
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertArrayEquals(serialized, expected.serialize())
    assertTrue(actual.data.isEmpty())
  }

  @Test
  fun `parse and serialize with 1 byte of response data`() {
    val serialized = byteArrayOf(BYTE_3, BYTE_1, BYTE_2)
    val expected = ApduResponse(BYTE_1, BYTE_2, byteArrayOf(BYTE_3))

    val actual = ApduResponse.parse(serialized)
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `parse and serialize with multiple bytes of response data`() {
    val serialized = byteArrayOf(BYTE_3, BYTE_4, BYTE_5, BYTE_1, BYTE_2)
    val expected = ApduResponse(
      BYTE_1,
      BYTE_2,
      byteArrayOf(BYTE_3, BYTE_4, BYTE_5)
    )

    val actual = ApduResponse.parse(serialized)
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertArrayEquals(serialized, expected.serialize())
  }

  @Test
  fun `throw errors when parsing bad responses`() {
    assertThrows(IllegalArgumentException::class.java) {
      ApduResponse.parse(byteArrayOf())
    }

    assertThrows(IllegalArgumentException::class.java) {
      ApduResponse.parse(byteArrayOf(BYTE_1))
    }
  }

  @Test
  fun `test the isSuccessful method`() {
    assertTrue(ApduResponse(b(0x90), b(0x00)).isSuccessful)

    assertFalse(ApduResponse(b(0x90), b(0x42)).isSuccessful)
    assertFalse(ApduResponse(b(0x80), b(0x00)).isSuccessful)
  }

  @Test
  fun `test the toString method`() {
    assertEquals(
      "ApduResponse(sw1=0x90, sw2=0xFA, data=[CAFE])",
      ApduResponse(b(0x90), b(0xfa), data = BYTE_ARRAY).toString()
    )
  }

  companion object {
    val BYTE_1 = b(23)
    val BYTE_2 = b(29)
    val BYTE_3 = b(31)
    val BYTE_4 = b(37)
    val BYTE_5 = b(41)
    val BYTE_ARRAY = byteArrayOf(b(0xca), b(0xfe))
  }
}
