package com.example.wmtsmap.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Replaceable asynchronous cache for encoded raster tile bytes. */
interface WmtsTileCache {
    suspend fun get(key: String): ByteArray?
    suspend fun put(key: String, value: ByteArray)
    suspend fun invalidate()
}

/** A bounded least-recently-used in-memory tile cache. */
class InMemoryWmtsTileCache(
    private val maxEntries: Int,
) : WmtsTileCache {
    private val mutex = Mutex()
    private val entries = LinkedHashMap<String, ByteArray>()

    init {
        require(maxEntries > 0)
    }

    override suspend fun get(key: String): ByteArray? = mutex.withLock {
        entries.remove(key)?.also { entries[key] = it }?.copyOf()
    }

    override suspend fun put(key: String, value: ByteArray) {
        mutex.withLock {
            entries.remove(key)
            entries[key] = value.copyOf()
            while (entries.size > maxEntries) {
                val eldestKey = entries.entries.iterator().next().key
                entries.remove(eldestKey)
            }
        }
    }

    override suspend fun invalidate() {
        mutex.withLock {
            entries.clear()
        }
    }
}
