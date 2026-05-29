/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import android.content.Context
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get

fun Context.personalLibraryCredentials() =
    PersonalLibraryCredentials(
        serverUrl = dataStore.get(PersonalLibraryServerUrlKey, ""),
        username = dataStore.get(PersonalLibraryUsernameKey, ""),
        password = dataStore.get(PersonalLibraryPasswordKey, ""),
    )
