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

package fr.acinq.phoenix.android.components.contact

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acinq.bitcoin.utils.Try
import fr.acinq.lightning.utils.UUID
import fr.acinq.lightning.utils.currentTimestampMillis
import fr.acinq.lightning.wire.OfferTypes
import fr.acinq.phoenix.android.R
import fr.acinq.phoenix.android.business
import fr.acinq.phoenix.android.components.Button
import fr.acinq.phoenix.android.components.FilledButton
import fr.acinq.phoenix.android.components.SwitchView
import fr.acinq.phoenix.android.components.TextInput
import fr.acinq.phoenix.android.components.dialogs.ModalBottomSheet
import fr.acinq.phoenix.android.components.scanner.ScannerView
import fr.acinq.phoenix.data.ContactInfo
import fr.acinq.phoenix.data.ContactOffer
import kotlinx.coroutines.launch


/**
 * A contact detail is a bottom sheet dialog that displays the contact's name, photo, and
 * associated offers/lnids.
 *
 * The contact can be edited and deleted from that screen.
 */
@Composable
fun SaveNewContactDialog(
    initialOffer: OfferTypes.Offer?,
    onDismiss: () -> Unit,
    onSaved: (ContactInfo) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var offer by remember { mutableStateOf(initialOffer?.encode() ?: "") }
    var offerErrorMessage by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var useOfferKey by remember { mutableStateOf(true) }

    val contactsDb by business.databaseManager.contactsDb.collectAsState(null)

    ModalBottomSheet(
        onDismiss = onDismiss,
        skipPartiallyExpanded = true,
        horizontalAlignment = Alignment.CenterHorizontally,
        internalPadding = PaddingValues(top = 0.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
    ) {
        var showScannerView by remember { mutableStateOf(false) }
        if (showScannerView) {
            OfferScanner(
                onScannerDismiss = { showScannerView = false },
                onOfferScanned = {
                    offer = it
                    offerErrorMessage = ""
                    showScannerView = false
                }
            )
        } else {
            Text(text = stringResource(id = R.string.contact_add_title), style = MaterialTheme.typography.h4, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            ContactPhotoView(photoUri = photoUri, name = name, onChange = { photoUri = it }, imageSize = 120.dp, borderSize = 4.dp)
            Spacer(modifier = Modifier.height(24.dp))
            TextInput(
                text = name,
                onTextChange = { name = it },
                textStyle = MaterialTheme.typography.h3,
                staticLabel = stringResource(id = R.string.contact_name_label),
            )
            TextInput(
                text = offer,
                onTextChange = { offer = it },
                enabled = initialOffer == null,
                enabledEffect = false,
                staticLabel = stringResource(id = R.string.contact_offer_label),
                trailingIcon = {
                    Button(
                        onClick = { showScannerView = true },
                        icon = R.drawable.ic_scan_qr,
                        iconTint = MaterialTheme.colors.primary
                    )
                },
                maxLines = 4,
                errorMessage = offerErrorMessage,
                showResetButton = false
            )
            Spacer(modifier = Modifier.height(8.dp))
            SwitchView(
                text = stringResource(id = R.string.contact_offer_key_title),
                description = if (useOfferKey) {
                    stringResource(id = R.string.contact_offer_key_enabled)
                } else {
                    stringResource(id = R.string.contact_offer_key_disabled)
                },
                checked = useOfferKey,
                onCheckedChange = { useOfferKey = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
            FilledButton(
                text = stringResource(id = R.string.contact_add_contact_button),
                icon = R.drawable.ic_check,
                onClick = {
                    contactsDb?.let { db ->
                        scope.launch {
                            offerErrorMessage = ""
                            when (val res = OfferTypes.Offer.decode(offer)) {
                                is Try.Success -> {
                                    val decodedOffer = res.result
                                    val existingContact = db.contactForOffer(decodedOffer)
                                    if (existingContact != null) {
                                        offerErrorMessage = context.getString(R.string.contact_error_offer_known, existingContact.name)
                                    } else {
                                        val contactOffer = ContactOffer(res.result, label = null, createdAt = currentTimestampMillis())
                                        val newContact = ContactInfo(
                                            id = UUID.randomUUID(),
                                            name = name,
                                            photoUri = photoUri,
                                            useOfferKey = useOfferKey,
                                            offers = listOf(contactOffer),
                                            addresses = emptyList()
                                        )
                                        db.saveContact(newContact)
                                        onSaved(newContact)
                                    }
                                }
                                is Try.Failure -> { offerErrorMessage = context.getString(R.string.contact_error_offer_invalid) }
                            }
                        }
                    }
                },
                shape = CircleShape,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun OfferScanner(
    onScannerDismiss: () -> Unit,
    onOfferScanned: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScannerView(onScannedText = onOfferScanned, isPaused = false, onDismiss = onScannerDismiss)
    }
}
