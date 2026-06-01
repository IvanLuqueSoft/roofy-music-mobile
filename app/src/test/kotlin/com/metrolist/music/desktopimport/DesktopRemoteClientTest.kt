package com.metrolist.music.desktopimport

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopRemoteClientTest {
    @Test
    fun buildWebSocketUrl_convertsHttpRemoteUrl() {
        assertEquals(
            "ws://192.168.1.10:4333/?token=remote-token",
            DesktopRemoteClient.buildWebSocketUrl(
                remoteUrl = "http://192.168.1.10:4333/?token=remote-token",
                token = "",
            ),
        )
    }

    @Test
    fun buildWebSocketUrl_appendsTokenWhenMissingFromUrl() {
        assertEquals(
            "wss://remote.example.com/?token=remote-token",
            DesktopRemoteClient.buildWebSocketUrl(
                remoteUrl = "https://remote.example.com/",
                token = "remote-token",
            ),
        )
    }

    @Test
    fun normalizeDurationToMs_acceptsSecondsFromDesktop() {
        assertEquals(215_000L, DesktopRemoteClient.normalizeDurationToMs(215.0))
    }

    @Test
    fun normalizeDurationToMs_acceptsMillisecondsFromDesktop() {
        assertEquals(215_000L, DesktopRemoteClient.normalizeDurationToMs(215_000.0))
    }
}
