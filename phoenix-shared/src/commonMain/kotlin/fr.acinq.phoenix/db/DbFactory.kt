/*
 * Copyright 2020 ACINQ SAS
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

package fr.acinq.phoenix.db

import app.cash.sqldelight.db.SqlDriver
import fr.acinq.bitcoin.Chain
import fr.acinq.lightning.logging.LoggerFactory
import fr.acinq.phoenix.utils.PlatformContext

expect fun createChannelsDbDriver(ctx: PlatformContext, chain: Chain, nodeIdHash: String): SqlDriver

expect fun createPaymentsDbDriver(ctx: PlatformContext, chain: Chain, nodeIdHash: String, onError: (String) -> Unit): SqlDriver

expect fun createAppDbDriver(ctx: PlatformContext): SqlDriver
