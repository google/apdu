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
class ByteToolsTest {
  @Test
  fun `bytesToInt converts bytes to ints`() {
    assertEquals(0x00, bytesToInt(b(0x00), b(0x00)))
    assertEquals(0xff, bytesToInt(b(0x00), b(0xff)))
    assertEquals(0xf00, bytesToInt(b(0x0f), b(0x00)))
    assertEquals(0xcafe, bytesToInt(b(0xca), b(0xfe)))
  }

  @Test
  fun `asUnsignedInt treats the Byte as an unsigned integer`() {
    assertEquals(0, b(0x00).asUnsignedInt())
    assertEquals(127, b(0x7f).asUnsignedInt())
    assertEquals(128, b(0x80).asUnsignedInt())
    assertEquals(255, b(0xff).asUnsignedInt())
  }

  @Test
  fun `toHexString outputs a Byte as a hex string`() {
    assertEquals("00", b(0x00).toHexString())
    assertEquals("0F", b(0x0f).toHexString())
    assertEquals("7F", b(0x7f).toHexString())
    assertEquals("FF", b(0xff).toHexString())
  }

  @Test
  fun `toHexString outputs a ByteArray as a hex string`() {
    assertEquals("", byteArrayOf().toHexString())
    assertEquals("00", byteArrayOf(b(0x00)).toHexString())
    assertEquals("7A", byteArrayOf(b(0x7A)).toHexString())
    assertEquals("0000", byteArrayOf(b(0x00), b(0x00)).toHexString())
    assertEquals("00FF", byteArrayOf(b(0x00), b(0xFF)).toHexString())
    assertEquals("CAFE", byteArrayOf(b(0xca), b(0xfe)).toHexString())
    assertEquals(
      "0123456789",
      byteArrayOf(b(0x01), b(0x23), b(0x45), b(0x67), b(0x89)).toHexString()
    )
  }
}
