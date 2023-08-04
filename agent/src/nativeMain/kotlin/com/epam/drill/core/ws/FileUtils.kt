/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.core.ws

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

fun AsyncStreamBase.toAsyncStream(position: Long = 0L): AsyncStream =
    AsyncStream(this, position)

@SharedImmutable
internal val IOWorker = Worker.start()

internal suspend fun fileWrite(file: CPointer<FILE>, position: Long, buffer: ByteArray, offset: Int, len: Int) {
    if (len > 0) {
        fileWrite(file, position, buffer.copyOfRange(offset, offset + len))
    }
}

suspend inline fun <T : AsyncCloseable, TR> T.use(callback: T.() -> TR): TR {
    var error: Throwable? = null
    val result = try {
        callback()
    } catch (e: Throwable) {
        error = e
        null
    }
    close()
    if (error != null) throw error
    return result!!
}


interface AsyncOutputStream : AsyncBaseStream {
    suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
}

interface AsyncBaseStream : AsyncCloseable

interface AsyncCloseable {
    suspend fun close()

    companion object
}

suspend fun open(rpath: String, mode: String): AsyncStream {
    var fd: CPointer<FILE>? = fileOpen(rpath, mode)
    val errno = posix_errno()
    //if (fd == null || errno != 0) {
    if (fd == null) {
        val errstr = strerror(errno)?.toKString()
        throw RuntimeException("Can't open '$rpath' with mode '$mode' errno=$errno, errstr=$errstr")
    }

    fun checkFd() {
        if (fd == null) error("Error with file '$rpath'")
    }

    return object : AsyncStreamBase() {
        override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
            checkFd()
            return 0
        }

        override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
            checkFd()
            return fileWrite(fd!!, position, buffer, offset, len)
        }

        override suspend fun setLength(value: Long) {
            checkFd()
            fileSetLength(rpath, value)
        }

        override suspend fun getLength(): Long {
            checkFd()
            return fileLength(fd!!)
        }

        override suspend fun close() {
            if (fd != null) {
                fileClose(fd!!)
            }
            fd = null
        }

        override fun toString(): String = "($rpath)"
    }.toAsyncStream()
}

interface AsyncInputStreamWithLength : AsyncInputStream,
    AsyncGetPositionStream,
    AsyncGetLengthStream

interface AsyncGetPositionStream : AsyncBaseStream {
    suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncPositionLengthStream : AsyncPositionStream,
    AsyncLengthStream

interface AsyncPositionStream : AsyncGetPositionStream {
    suspend fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
}

class AsyncStream(private val base: AsyncStreamBase, private var position: Long = 0L) : AsyncInputStream,
    AsyncInputStreamWithLength,
    AsyncOutputStream,
    AsyncPositionLengthStream,
    AsyncCloseable {
    private val readQueue = AsyncThread()
    private val writeQueue = AsyncThread()

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue {
        val read = base.read(position, buffer, offset, len)
        if (read >= 0) position += read
        read
    }

    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
        base.write(position, buffer, offset, len)
        position += len
    }

    override suspend fun setPosition(value: Long): Unit = run { this.position = value }
    override suspend fun getPosition(): Long = this.position
    override suspend fun setLength(value: Long): Unit = base.setLength(value)
    override suspend fun getLength(): Long = base.getLength()
    suspend fun size(): Long = base.getLength()
    override suspend fun close(): Unit = base.close()

}

interface AsyncInvokable {
    suspend operator fun <T> invoke(func: suspend () -> T): T
}

class AsyncThread : AsyncInvokable {
    private var lastPromise: Deferred<*> = CompletableDeferred(Unit).apply {
        this.complete(Unit)
    }

    override suspend operator fun <T> invoke(func: suspend () -> T): T {
        val task = sync(coroutineContext, func)
        try {
            return task.await()
        } catch (e: Throwable) {
            throw e
        }
    }


    fun <T> sync(context: CoroutineContext, func: suspend () -> T): Deferred<T> {
        val oldPromise = lastPromise
        val promise = asyncImmediately(context) {
            oldPromise.await()
            func()
        }
        lastPromise = promise
        return promise

    }
}

fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) =
    CoroutineScope(context).asyncImmediately(callback)

fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T) = _async(CoroutineStart.UNDISPATCHED, callback)
private fun <T> CoroutineScope._async(start: CoroutineStart, callback: suspend () -> T): Deferred<T> =
    async(coroutineContext, start = start) {
        try {
            callback()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }


open class AsyncStreamBase : AsyncCloseable,
    AsyncRAInputStream,
    AsyncRAOutputStream,
    AsyncLengthStream {
    //var refCount = 0

    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
        throw UnsupportedOperationException()

    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit =
        throw UnsupportedOperationException()

    override suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
    override suspend fun getLength(): Long = throw UnsupportedOperationException()

    override suspend fun close(): Unit = Unit
}

interface AsyncRAInputStream {
    suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncRAOutputStream {
    suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int)
}

interface AsyncInputStream : AsyncBaseStream {
    suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncLengthStream : AsyncGetLengthStream {
    suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
}

interface AsyncGetLengthStream : AsyncBaseStream {
    suspend fun getLength(): Long = throw UnsupportedOperationException()
}


suspend fun <T, R> executeInWorker(worker: Worker, value: T, func: (T) -> R): R {
    class Info(val value: T, val func: (T) -> R)

    val info = Info(value.freeze(), func.freeze())
    val future =
        worker.execute(TransferMode.UNSAFE, { info }, { it: Info -> it.func(it.value) })
    return future.await()
}

suspend fun <T> Future<T>.await(): T {
    var n = 0
    while (this.state != FutureState.COMPUTED) {
        when (this.state) {
            FutureState.INVALID -> error("Error in worker")
            FutureState.CANCELLED -> throw CancellationException("cancelled")
            FutureState.THROWN -> error("Worker thrown exception")
            else -> delay(((n++).toDouble() / 3.0).toLong())
        }
    }
    return this.result
}


internal suspend fun fileClose(file: CPointer<FILE>): Unit = executeInWorker(
    IOWorker,
    file
) { fd ->
    fclose(fd)
    Unit
}

internal suspend fun fileOpen(name: String, mode: String): CPointer<FILE>? {
    data class Info(val name: String, val mode: String)
    return executeInWorker(
        IOWorker,
        Info(name, mode)
    ) {
        fopen(it.name, it.mode)
    }
}

expect suspend fun fileSetLength(file: String, length: Long)
expect suspend fun fileLength(file: CPointer<FILE>): Long
expect suspend fun fileWrite(file: CPointer<FILE>, position: Long, data: ByteArray): Long
