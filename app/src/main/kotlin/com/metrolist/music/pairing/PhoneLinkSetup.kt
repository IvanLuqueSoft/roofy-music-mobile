/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.pairing

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.music.R
import com.metrolist.music.constants.DesktopImportEndpointUrlKey
import com.metrolist.music.constants.DesktopImportTokenKey
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.device.DeviceSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhoneLinkSetup {
    suspend fun applyPairingUri(
        context: Context,
        dataStore: DataStore<Preferences>,
        uri: Uri,
    ): Boolean {
        when {
            RoofyPairingLinks.isDevicePairLink(uri) -> {
                val params = RoofyPairingLinks.parseDevicePair(uri) ?: return false
                if (!DeviceSessionManager.applyDevicePair(params)) {
                    return false
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.phone_link_paired, Toast.LENGTH_SHORT).show()
                }
                return true
            }
            RoofyPairingLinks.isSubsonicPairLink(uri) -> {
                val subsonic = RoofyPairingLinks.parseSubsonicPair(uri) ?: return false
                dataStore.edit { settings ->
                    settings[PersonalLibraryEnabledKey] = true
                    settings[PersonalLibraryServerUrlKey] = subsonic.serverUrl
                    settings[PersonalLibraryUsernameKey] = subsonic.username
                    settings[PersonalLibraryPasswordKey] = subsonic.password
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.personal_library_paired, Toast.LENGTH_SHORT).show()
                }
                return true
            }
            RoofyPairingLinks.isImportPairLink(uri) -> {
                val import = RoofyPairingLinks.parseImportPair(uri) ?: return false
                dataStore.edit { settings ->
                    settings[DesktopImportEndpointUrlKey] = import.endpointUrl
                    settings[DesktopImportTokenKey] = import.token
                }
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            context,
                            R.string.desktop_import_paired,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
                return true
            }
            else -> return false
        }
    }
}
