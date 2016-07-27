package com.github.shyiko.ktlint

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

fun ASTNode.visit(cb: (node: ASTNode) -> Unit) { cb(this); this.getChildren(null).forEach { it.visit(cb) } }

fun findErrorElement(element: PsiElement): PsiErrorElement? {
    if (element is PsiErrorElement) {
        return element
    }
    element.children.forEach {
        val errorElement = findErrorElement(it)
        if (errorElement != null) {
            return errorElement
        }
    }
    return null
}

fun <T>List<T>.head() = this.subList(0, this.size - 1)
fun <T>List<T>.tail() = this.subList(1, this.size)

fun <T>Sequence<Callable<T>>.parallel(cb: (T) -> Unit,
    numberOfThreads: Int = Runtime.getRuntime().availableProcessors()) {
    val q = ArrayBlockingQueue<Future<T>>(numberOfThreads)
    val pill = object : Future<T> {

        override fun isDone(): Boolean { throw UnsupportedOperationException() }
        override fun get(timeout: Long, unit: TimeUnit): T { throw UnsupportedOperationException() }
        override fun get(): T { throw UnsupportedOperationException() }
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean { throw UnsupportedOperationException() }
        override fun isCancelled(): Boolean { throw UnsupportedOperationException() }
    }
    val consumer = Thread(Runnable {
        while (true) {
            val future = q.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
            if (future === pill) {
                break
            }
            cb(future.get())
        }
    })
    consumer.start()
    val executorService = Executors.newCachedThreadPool()
    for (v in this) {
        q.put(executorService.submit(v))
    }
    q.put(pill)
    executorService.shutdown()
    consumer.join()
}
