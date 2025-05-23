/*
 * Copyright 2024 ACINQ SAS
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

package fr.acinq.phoenix.utils.extensions

import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.io.ByteArrayOutput
import fr.acinq.lightning.serialization.OutputExtensions.writeUuid
import fr.acinq.lightning.utils.UUID

fun ByteVector32.deriveUUID(): UUID = UUID.fromBytes(this.take(16).toByteArray())

// TODO: use standard Uuid once migrated to kotlin 2
fun UUID.toByteArray() =
    ByteArrayOutput().run {
        writeUuid(this@toByteArray)
        toByteArray()
    }