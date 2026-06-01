package com.metrolist.music.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RoofyPairingLinksTest {
    @Test
    fun parseSubsonicPairQuery_validParams() {
        val params =
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "http://192.168.1.2:4533",
                username = "roofy",
                password = "secret",
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:4533", params!!.serverUrl)
        assertEquals("roofy", params.username)
        assertEquals("secret", params.password)
    }

    @Test
    fun parseSubsonicPairQuery_trimsWhitespace() {
        val params =
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "  http://localhost/  ",
                username = " user ",
                password = "pass",
            )
        assertNotNull(params)
        assertEquals("http://localhost", params!!.serverUrl)
        assertEquals("user", params.username)
    }

    @Test
    fun parseSubsonicPairQuery_rejectsMissingPassword() {
        assertNull(
            RoofyPairingLinks.parseSubsonicPairQuery(
                serverUrl = "http://localhost",
                username = "roofy",
                password = "",
            ),
        )
    }

    @Test
    fun parseDevicePairQuery_validParams() {
        val params =
            RoofyPairingLinks.parseDevicePair(
                android.net.Uri.parse(
                    "roofymusic://pair/device" +
                        "?serverUrl=http%3A%2F%2F192.168.1.2%3A4533" +
                        "&username=roofy" +
                        "&password=secret" +
                        "&computerName=Studio+PC" +
                        "&endpointUrl=http%3A%2F%2F192.168.1.2%3A8765" +
                        "&token=abc123",
                ),
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:4533", params!!.serverUrl)
        assertEquals("roofy", params.username)
        assertEquals("secret", params.password)
        assertEquals("Studio PC", params.computerName)
        assertEquals("http://192.168.1.2:8765", params.endpointUrl)
        assertEquals("abc123", params.token)
        assertNull(params.webControlUrl)
        assertNull(params.remoteControlUrl)
        assertNull(params.remoteControlToken)
    }

    @Test
    fun parseDevicePairQuery_includesRemoteControlUrl() {
        val params =
            RoofyPairingLinks.parseDevicePair(
                android.net.Uri.parse(
                    "roofymusic://pair/device" +
                        "?serverUrl=http%3A%2F%2F192.168.1.2%3A4533" +
                        "&username=roofy" +
                        "&password=secret" +
                        "&endpointUrl=http%3A%2F%2F192.168.1.2%3A8765" +
                        "&token=abc123" +
                        "&remoteControlUrl=http%3A%2F%2F192.168.1.2%3A4333%2F%3Ftoken%3Dremote" +
                        "&remoteControlToken=remote" +
                        "&webControlUrl=http%3A%2F%2F192.168.1.2%3A4333%2F%3Ftoken%3Dremote",
                ),
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:4333/?token=remote", params!!.remoteControlUrl)
        assertEquals("remote", params.remoteControlToken)
        assertEquals("http://192.168.1.2:4333/?token=remote", params.webControlUrl)
    }

    @Test
    fun parseDevicePairQuery_usesWebControlAsLegacyRemoteFallback() {
        val params =
            RoofyPairingLinks.parseDevicePair(
                android.net.Uri.parse(
                    "roofymusic://pair/device" +
                        "?serverUrl=http%3A%2F%2F192.168.1.2%3A4533" +
                        "&username=roofy" +
                        "&password=secret" +
                        "&endpointUrl=http%3A%2F%2F192.168.1.2%3A8765" +
                        "&token=abc123" +
                        "&webControlUrl=http%3A%2F%2F192.168.1.2%3A4333%2F%3Ftoken%3Dremote",
                ),
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:4333/?token=remote", params!!.remoteControlUrl)
        assertEquals("remote", params.remoteControlToken)
    }

    @Test
    fun parseImportPairQuery_validParams() {
        val params =
            RoofyPairingLinks.parseImportPairQuery(
                endpointUrl = "http://192.168.1.2:8765/",
                token = "abc123",
            )
        assertNotNull(params)
        assertEquals("http://192.168.1.2:8765", params!!.endpointUrl)
        assertEquals("abc123", params.token)
    }
}
