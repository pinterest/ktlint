package io.github.ktlint.core.cli.internal

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Executes "Callable"s in parallel (lazily).
 * The results are gathered one-by-one (by `cb(<callable result>)`) in the order of corresponding "Callable"s
 * in the "Sequence" (think `seq.toList().map { executorService.submit(it) }.forEach { cb(it.get()) }` but without
 * buffering an entire sequence).
 *
 * Once kotlinx-coroutines are out of "experimental" stage everything below can be replaced with
 * ```
 * suspend fun <T> Sequence<Callable<T>>.parallel(...) {
 *     val ctx = newFixedThreadPoolContext(numberOfThreads, "Sequence<Callable<T>>.parallel")
 *     ctx.use {
 *         val channel = produce(ctx, numberOfThreads) {
 *             for (task in this@parallel) {
 *                 send(async(ctx) { task.call() })
 *             }
 *         }
 *         for (res in channel) {
 *             cb(res.await())
 *         }
 *     }
 * }
 * ```
 */
internal fun <T> Sequence<Callable<T>>.parallel(
    cb: (T) -> Unit,
    numberOfThreads: Int = Runtime.getRuntime().availableProcessors(),
) {
    val pill =
        object : Future<T> {
            override fun isDone(): Boolean = throw UnsupportedOperationException()

            override fun get(
                timeout: Long,
                unit: TimeUnit,
            ): T = throw UnsupportedOperationException()

            override fun get(): T = throw UnsupportedOperationException()

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = throw UnsupportedOperationException()

            override fun isCancelled(): Boolean = throw UnsupportedOperationException()
        }
    val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
    val producer =
        thread(start = true) {
            val executorService = Executors.newCachedThreadPool()
            try {
                for (task in this) {
                    q.put(executorService.submit(task))
                }
                q.put(pill)
            } catch (_: InterruptedException) {
                // we've been asked to stop consuming sequence
            } finally {
                executorService.shutdown()
            }
        }
    try {
        while (true) {
            val result = q.take()
            if (result != pill) cb(result.get()) else break
        }
    } finally {
        producer.interrupt() // in case q.take()/result.get() throws
        producer.join()
    }
}
