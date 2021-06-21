package com.lzx.multiple.impl

import android.os.Build
import android.os.Process
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.lzx.multiple.MultipleUpload
import com.lzx.multiple.MultipleUploadObserver
import com.lzx.multiple.MultipleUploadStateLiveData
import com.lzx.multiple.OnMultipleUploadState
import com.lzx.multiple.OnSingleUploadState
import com.lzx.multiple.SingleUploadObserver
import com.lzx.multiple.UploadStateLiveData
import com.lzx.multiple.multipleUploadObserver
import com.lzx.multiple.singleUploadObserver
import com.lzx.multiple.upload.UploadInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

abstract class UploadBuilder(private val owner: LifecycleOwner?) {

    internal var uploadNut: UploadInterface? = null
    internal var filter: UploadFilter? = null
    internal var supportDispatcher: ExecutorCoroutineDispatcher
    private var singleLiveData = UploadStateLiveData()
    private var multipleLiveData = MultipleUploadStateLiveData()

    internal abstract fun asyncRun(scope: CoroutineScope?,
                                   singleLiveData: UploadStateLiveData,
                                   multipleLiveData: MultipleUploadStateLiveData)

    init {
        val corePoolSize = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> 2
            else -> 1
        }
        val threadPoolExecutor = ThreadPoolExecutor(corePoolSize, corePoolSize,
            5L, TimeUnit.SECONDS, LinkedBlockingQueue(), UploadThreadFactory())
        threadPoolExecutor.allowCoreThreadTimeOut(true)
        supportDispatcher = threadPoolExecutor.asCoroutineDispatcher()
    }

    /**
     * 配置线程池
     */
    fun setThreadPoolExecutor(executor: ThreadPoolExecutor) = apply {
        supportDispatcher = executor.asCoroutineDispatcher()
    }

    /**
     * 配置具体上传实现
     */
    fun setUploadImpl(upload: UploadInterface) = apply {
        uploadNut = upload
    }

    /**
     * 配置url过滤器
     */
    fun filter(filter: UploadFilter) = apply {
        this.filter = filter
    }

    /**
     * 单个上传监听，dsl形式
     */
    fun singleUploadObserver(observer: SingleUploadObserver.() -> Unit) = apply {
        singleLiveData.singleUploadObserver(owner, observer)
    }

    /**
     * 单个上传监听，接口形式
     */
    fun singleUploadObserver(state: OnSingleUploadState) = apply {
        owner?.let { it ->
            singleLiveData.observe(it, { state.uploadState(it) })
        }
    }

    /**
     * 多个上传监听，dsl形式
     */
    fun multipleUploadObserver(observer: MultipleUploadObserver.() -> Unit) = apply {
        multipleLiveData.multipleUploadObserver(owner, observer)
    }

    /**
     * 多个上传监听，接口形式
     */
    fun multipleUploadObserver(state: OnMultipleUploadState) = apply {
        owner?.let { it ->
            multipleLiveData.observe(it, { state.uploadState(it) })
        }
    }

    /**
     * 发起上传
     */
    fun upload() {
        val time = measureTimeMillis {
            asyncRun(owner?.lifecycleScope, singleLiveData, multipleLiveData)
        }
        Log.i(MultipleUpload.TAG, "上传总时间 = $time")
    }
}

/**
 * 上传前过滤
 */
interface UploadFilter {
    fun apply(path: String): Boolean
}

/**
 * 线程
 */
class UploadThreadFactory : ThreadFactory {
    private val group: ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    companion object {
        private val poolNumber = AtomicInteger(1)
        private const val DEFAULT_PRIORITY =
            Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE
    }

    init {
        val s = System.getSecurityManager()
        group = s?.threadGroup ?: Thread.currentThread().threadGroup
        namePrefix = "Upload-${poolNumber.getAndIncrement()}-thread-"
    }

    override fun newThread(r: Runnable): Thread {
        val thread = object : Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0) {
            override fun run() {
                Process.setThreadPriority(DEFAULT_PRIORITY)
                super.run()
            }
        }
        if (thread.isDaemon) thread.isDaemon = false
        if (thread.priority != Thread.NORM_PRIORITY) thread.priority = Thread.NORM_PRIORITY
        return thread
    }
}