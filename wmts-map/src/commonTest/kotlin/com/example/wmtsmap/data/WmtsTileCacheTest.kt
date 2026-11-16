package com.example.wmtsmap.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class WmtsTileCacheTest {
    @Test
    fun `cache miss hit invalidation and LRU eviction`() = runTest {
        val cache = InMemoryWmtsTileCache(maxEntries = 2)

        assertNull(cache.get("missing"))
        cache.put("a", byteArrayOf(1))
        cache.put("b", byteArrayOf(2))
        assertContentEquals(byteArrayOf(1), cache.get("a"))
        cache.put("c", byteArrayOf(3))
        assertNull(cache.get("b"))
        assertContentEquals(byteArrayOf(1), cache.get("a"))
        cache.invalidate()
        assertNull(cache.get("a"))
        assertNull(cache.get("c"))
    }

    @Test
    fun `cache copies byte arrays at its boundary`() = runTest {
        val cache = InMemoryWmtsTileCache(maxEntries = 1)
        val source = byteArrayOf(1)
        cache.put("tile", source)
        source[0] = 2
        val retrieved = cache.get("tile")
        retrieved?.set(0, 3)

        assertContentEquals(byteArrayOf(1), cache.get("tile"))
    }
}
