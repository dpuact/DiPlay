package com.shilapi.xcertplay.network

import java.net.Inet6Address
import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

class WirelessHostAddressTest {
    @Test fun dualStackManualApUsesIpv4RegardlessOfAddressOrder() {
        // Matches the failing Harman uap0 diagnostic: one usable IPv4 and one
        // scoped link-local IPv6. V3 picked IPv6 and never discovered a peer.
        val ipv4 = ip("192.168.43.1")
        val ipv6 = Inet6Address.getByAddress(null, ip("fe80::1234").address, 7)
        assertEquals(ipv4, wirelessHostAddress(listOf(ipv4, ipv6), 7))
        assertEquals(ipv4, wirelessHostAddress(listOf(ipv6, ipv4), 7))
    }

    @Test fun replacesScopeFromAnotherInterface() {
        val wrongScope = Inet6Address.getByAddress(null, ip("fe80::1234").address, 3)
        assertEquals(8, (wirelessHostAddress(listOf(wrongScope), 8) as Inet6Address).scopeId)
    }

    @Test fun selectsIpv4WithoutLinkLocalOrInterfaceScope() {
        val ipv4 = ip("192.168.43.1")
        assertEquals(ipv4, wirelessHostAddress(listOf(ip("::1"), ip("2001:db8::1"), ipv4), 7))
        assertEquals(ipv4, wirelessHostAddress(listOf(ip("fe80::1234"), ipv4), 0))
        assertNull(wirelessHostAddress(listOf(ip("0.0.0.0"), ip("127.0.0.1"), ip("224.0.0.251")), 7))
    }

    @Test fun invalidIpv4CandidatesDoNotHideTheUsableApAddress() {
        val ipv4 = ip("172.20.10.1")
        assertEquals(ipv4, wirelessHostAddress(listOf(
            ip("0.0.0.0"), ip("127.0.0.1"), ip("169.254.1.1"), ip("224.0.0.251"),
            ip("fe80::1234"), ipv4,
        ), 7))
    }

    @Test fun ipv6OnlyApRetainsScopedFallback() {
        val result = wirelessHostAddress(listOf(ip("169.254.1.1"), ip("fe80::1234")), 7) as Inet6Address
        assertTrue(result.isLinkLocalAddress)
        assertEquals(7, result.scopeId)
        assertNull(wirelessHostAddress(listOf(ip("fe80::1234")), 0))
    }

    private fun ip(value: String) = InetAddress.getByName(value)
}
