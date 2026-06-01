package com.metrolist.music.device

import com.metrolist.music.pairing.DevicePairingParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSessionStoreTest {
    @Test
    fun fromDevicePairingParams_requiresRemoteFields() {
        val withoutRemote =
            DeviceSessionStore.fromDevicePairingParams(
                DevicePairingParams(
                    serverUrl = "http://192.168.1.2:4533",
                    username = "admin",
                    password = "secret",
                    endpointUrl = "http://192.168.1.2:60952",
                    token = "import",
                ),
            )
        assertNull(withoutRemote)

        val withRemote =
            DeviceSessionStore.fromDevicePairingParams(
                DevicePairingParams(
                    serverUrl = "http://192.168.1.2:4533",
                    username = "admin",
                    password = "secret",
                    endpointUrl = "http://192.168.1.2:60952",
                    token = "import",
                    computerName = "Desk",
                    remoteControlUrl = "http://192.168.1.2:4333/?token=remote",
                    remoteControlToken = "remote",
                ),
            )
        assertNotNull(withRemote)
        assertEquals("Desk", withRemote!!.computerName)
        assertEquals("remote", withRemote.remote.token)
    }
}
