/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.feature.map.terrain

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLock
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import platform.posix.memset
import platform.zlib.ZLIB_VERSION
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

actual fun TerrainTileFetcher(pmtilesUrl: String): TerrainTileFetcher = IosPmtilesTerrainTileFetcher(pmtilesUrl)

/**
 * iPhone/iPad: a small PMTiles v3 reader over HTTP range requests (NSURLSession), so only the bytes of the requested
 * tile are downloaded — the same behaviour as the Android/desktop reader.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosPmtilesTerrainTileFetcher(private val url: String) : TerrainTileFetcher {

    private class Header(
        val rootOffset: Long,
        val rootLength: Long,
        val leafOffset: Long,
        val tileDataOffset: Long,
        val internalGzip: Boolean,
        val tileGzip: Boolean,
    )

    private class Entry(val tileId: Long, val offset: Long, val length: Long, val runLength: Long)

    private val lock = NSLock()
    private var header: Header? = null
    private val directories = HashMap<Long, List<Entry>>()

    override fun fetchTile(zoom: Int, x: Int, y: Int): ByteArray? {
        val id = tileId(zoom, x, y)
        val h = loadHeader()
        var dirOffset = h.rootOffset
        var dirLength = h.rootLength
        repeat(MAX_DEPTH) {
            val entries = loadDirectory(h, dirOffset, dirLength)
            val entry = findEntry(entries, id) ?: return null
            if (entry.runLength > 0) {
                val raw = readRange(h.tileDataOffset + entry.offset, entry.length) ?: return null
                return if (h.tileGzip) gunzip(raw) else raw
            }
            dirOffset = h.leafOffset + entry.offset
            dirLength = entry.length
        }
        return null
    }

    override fun close() = Unit

    private fun loadHeader(): Header {
        lock.lock()
        try {
            header?.let {
                return it
            }
            val b = readRange(0, HEADER_BYTES) ?: error("PMTiles: no header")
            check(b.size >= HEADER_BYTES && b[0] == 'P'.code.toByte() && b[1] == 'M'.code.toByte()) {
                "PMTiles: bad header"
            }
            val parsed =
                Header(
                    rootOffset = le64(b, 8),
                    rootLength = le64(b, 16),
                    leafOffset = le64(b, 40),
                    tileDataOffset = le64(b, 56),
                    internalGzip = b[97].toInt() == COMPRESSION_GZIP,
                    tileGzip = b[98].toInt() == COMPRESSION_GZIP,
                )
            header = parsed
            return parsed
        } finally {
            lock.unlock()
        }
    }

    private fun loadDirectory(h: Header, offset: Long, length: Long): List<Entry> {
        lock.lock()
        try {
            directories[offset]?.let {
                return it
            }
        } finally {
            lock.unlock()
        }
        val raw = readRange(offset, length) ?: error("PMTiles: directory unavailable")
        val parsed = parseDirectory(if (h.internalGzip) gunzip(raw) else raw)
        lock.lock()
        try {
            directories[offset] = parsed
        } finally {
            lock.unlock()
        }
        return parsed
    }

    private fun parseDirectory(data: ByteArray): List<Entry> {
        var pos = 0

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val byte = data[pos++].toInt() and 0xFF
                result = result or ((byte and 0x7F).toLong() shl shift)
                if (byte and 0x80 == 0) return result
                shift += 7
            }
        }
        val count = varint().toInt()
        val ids = LongArray(count)
        var last = 0L
        for (i in 0 until count) {
            last += varint()
            ids[i] = last
        }
        val runs = LongArray(count) { varint() }
        val lengths = LongArray(count) { varint() }
        val entries = ArrayList<Entry>(count)
        var prevEnd = 0L
        for (i in 0 until count) {
            val raw = varint()
            val offset = if (raw == 0L && i > 0) prevEnd else raw - 1
            entries.add(Entry(ids[i], offset, lengths[i], runs[i]))
            prevEnd = offset + lengths[i]
        }
        return entries
    }

    private fun findEntry(entries: List<Entry>, id: Long): Entry? {
        var m = 0
        var n = entries.size - 1
        while (m <= n) {
            val k = (n + m) ushr 1
            val c = id - entries[k].tileId
            if (c > 0) {
                m = k + 1
            } else if (c < 0) {
                n = k - 1
            } else {
                return entries[k]
            }
        }
        if (n >= 0) {
            val e = entries[n]
            if (e.runLength == 0L || id - e.tileId < e.runLength) return e
        }
        return null
    }

    /** Hilbert-curve tile id, as the PMTiles v3 spec defines it. */
    private fun tileId(zoom: Int, x: Int, y: Int): Long {
        var acc = 0L
        for (z in 0 until zoom) acc += 1L shl (2 * z)
        val n = 1L shl zoom
        var tx = x.toLong()
        var ty = y.toLong()
        var d = 0L
        var s = n / 2
        while (s > 0) {
            val rx = if (tx and s > 0) 1L else 0L
            val ry = if (ty and s > 0) 1L else 0L
            d += s * s * ((3 * rx) xor ry)
            if (ry == 0L) {
                if (rx == 1L) {
                    tx = n - 1 - tx
                    ty = n - 1 - ty
                }
                val t = tx
                tx = ty
                ty = t
            }
            s /= 2
        }
        return acc + d
    }

    private fun le64(b: ByteArray, at: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[at + i].toLong() and 0xFF)
        return v
    }

    private fun readRange(offset: Long, length: Long): ByteArray? {
        if (length <= 0) return ByteArray(0)
        val nsUrl = NSURL.URLWithString(url) ?: return null
        val request = NSMutableURLRequest.requestWithURL(nsUrl)
        request.setValue("bytes=$offset-${offset + length - 1}", forHTTPHeaderField = "Range")
        request.setTimeoutInterval(REQUEST_TIMEOUT_SECONDS)
        val done = dispatch_semaphore_create(0)
        var result: ByteArray? = null
        val task =
            NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
                val status = (response as? NSHTTPURLResponse)?.statusCode ?: 0L
                // Only 206 is a real range answer; a 200 would be the whole archive.
                if (status == HTTP_PARTIAL && data != null && data.length > 0uL) {
                    result = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt())
                }
                dispatch_semaphore_signal(done)
            }
        task.resume()
        val deadline = dispatch_time(DISPATCH_TIME_NOW, (REQUEST_TIMEOUT_SECONDS.toLong() + 5) * NSEC_PER_SEC.toLong())
        if (dispatch_semaphore_wait(done, deadline) != 0L) {
            task.cancel()
            return null
        }
        return result
    }

    private fun gunzip(input: ByteArray): ByteArray = memScoped {
        val stream = alloc<z_stream>()
        memset(stream.ptr, 0, sizeOf<z_stream>().convert())
        // 15 + 16: a gzip wrapper.
        check(inflateInit2_(stream.ptr, GZIP_WINDOW_BITS, ZLIB_VERSION, sizeOf<z_stream>().toInt()) == Z_OK) {
            "zlib init failed"
        }
        val out = ChunkList()
        val chunk = ByteArray(CHUNK_BYTES)
        try {
            input.usePinned { pinnedIn ->
                stream.next_in = pinnedIn.addressOf(0).reinterpret()
                stream.avail_in = input.size.convert()
                while (true) {
                    var status = 0
                    var produced = 0
                    chunk.usePinned { pinnedOut ->
                        stream.next_out = pinnedOut.addressOf(0).reinterpret()
                        stream.avail_out = CHUNK_BYTES.convert()
                        status = inflate(stream.ptr, Z_NO_FLUSH)
                        produced = CHUNK_BYTES - stream.avail_out.toInt()
                    }
                    check(status == Z_OK || status == Z_STREAM_END) { "zlib error $status" }
                    out.add(chunk.copyOf(produced))
                    check(out.total <= MAX_DECOMPRESSED_BYTES) { "Decompressed terrain data too large" }
                    if (status == Z_STREAM_END) break
                    check(produced > 0 || stream.avail_in > 0u) { "Truncated gzip data" }
                }
            }
        } finally {
            inflateEnd(stream.ptr)
        }
        out.toByteArray()
    }

    private class ChunkList {
        private val parts = ArrayList<ByteArray>()
        var total = 0
            private set

        fun add(part: ByteArray) {
            parts.add(part)
            total += part.size
        }

        fun toByteArray(): ByteArray {
            val all = ByteArray(total)
            var at = 0
            for (p in parts) {
                p.copyInto(all, at)
                at += p.size
            }
            return all
        }
    }

    private companion object {
        const val HEADER_BYTES = 127L
        const val MAX_DEPTH = 4
        const val COMPRESSION_GZIP = 2
        const val HTTP_PARTIAL = 206L
        const val REQUEST_TIMEOUT_SECONDS = 30.0
        const val GZIP_WINDOW_BITS = 31
        const val CHUNK_BYTES = 64 * 1024
        const val MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024
    }
}
