package com.lzx.multiple

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.lzx.multiple.impl.MultipleUploadManager
import com.lzx.multiple.impl.SingleUploadManager
import com.lzx.multiple.impl.UploadBuilder
import java.io.File


/**
 * 通用多线程并行上传框架
 */
class MultipleUpload private constructor(private val owner: LifecycleOwner) {

    companion object {

        const val TAG = "MultipleUpload"

        @JvmStatic
        fun with(context: Context): MultipleUpload {
            if (context !is FragmentActivity) {
                throw IllegalStateException("context is not to FragmentActivity")
            }
            return with(context)
        }

        @JvmStatic
        fun with(activity: Activity): MultipleUpload {
            if (activity !is FragmentActivity) {
                throw IllegalStateException("activity is not to FragmentActivity")
            }
            return with(activity)
        }

        @JvmStatic
        fun with(context: FragmentActivity) = with(context as LifecycleOwner)

        @JvmStatic
        fun with(fragment: Fragment) = with(fragment.viewLifecycleOwner)

        @JvmStatic
        fun with(owner: LifecycleOwner = ProcessLifecycleOwner.get()) = MultipleUpload(owner)
    }

    /**
     * 加载单个上传文件
     */
    fun load(path: String?) = load(path, hashMapOf())

    /**
     * 加载多个上传文件
     */
    fun load(list: MutableList<String?>) = load(list, hashMapOf())

    /**
     * 加载单个上传文件
     * params:具体上传可能需求一些其他参数，可以放到params里面，然后在实现具体上传接口时获取
     */
    fun load(path: String?, params: HashMap<String, Any>): UploadBuilder {
        val list = mutableListOf<String?>()
        list.add(path)
        return load(list, params)
    }

    /**
     * 加载多个上传文件
     * params:具体上传可能需求一些其他参数，可以放到params里面，然后在实现具体上传接口时获取
     */
    fun load(list: MutableList<String?>, params: HashMap<String, Any>): UploadBuilder {
        val realList = mutableListOf<String>()
        list.filter { !it.isNullOrEmpty() && File(it).exists() }.forEach { realList.add(it!!) }
        return whatIfMap(realList.size == 1, whatIf = {
            SingleUploadManager(owner, realList[0], params)
        }, whatIfNot = {
            MultipleUploadManager(owner, realList, params)
        })
    }
}


typealias UploadStateLiveData = MutableLiveData<UploadState>

typealias MultipleUploadStateLiveData = MutableLiveData<MultipleUploadState>


/***单个上传状态 */
class UploadState {
    companion object {
        const val IDEA = "IDEA"
        const val Start = "Start"
        const val Progress = "Progress"
        const val Success = "Success"
        const val Fail = "Fail"
    }

    var currState = IDEA         //状态
    var progress: Int = 0        //进度
    var totalProgress: Int = 1   //进度
    var url: String? = null      //文件链接
    var errorCode: Int = -1      //失败信息
    var errMsg: String? = null  //失败信息
    var index: Int = 0          //当前上传第几个
    var otherParams: HashMap<String, Any>? = hashMapOf()  //上传成功后可能有一些其他的业务信息，通过这个获取
}

/***多个上传状态 */
class MultipleUploadState {
    companion object {
        const val IDEA = "IDEA"
        const val Start = "Start"
        const val Fail = "Fail"
        const val Completion = "Completion"
    }

    var currState = IDEA               //状态
    var catchIndex: Int = 0            //第几个上传失败了
    var errMsg: String? = null         //失败信息
    var successNum = 0                 //成功数量
    var failNum = 0                    //失败数量
    var urls = mutableListOf<String?>() //成功上传的url集合
}

class SingleUploadObserver {
    internal var start: ((index: Int) -> Unit)? = null
    internal var progress: ((index: Int, progress: Int, totalProgress: Int) -> Unit)? = null
    internal var success: ((index: Int, url: String?, otherParams: HashMap<String, Any>?) -> Unit)? = null
    internal var fail: ((index: Int, errCode: Int, errMsg: String?) -> Unit)? = null

    fun onStart(start: (index: Int) -> Unit) {
        this.start = start
    }

    fun onProgress(progress: (index: Int, progress: Int, totalProgress: Int) -> Unit) {
        this.progress = progress
    }

    fun onSuccess(success: (index: Int, url: String?, otherParams: HashMap<String, Any>?) -> Unit) {
        this.success = success
    }

    fun onFailure(fail: ((index: Int, errCode: Int, errStr: String?) -> Unit)) {
        this.fail = fail
    }
}

class MultipleUploadObserver {
    internal var start: (() -> Unit)? = null
    internal var completion: ((successNum: Int, failNum: Int, urls: MutableList<String?>) -> Unit)? = null
    internal var fail: ((catchIndex: Int, errMsg: String?) -> Unit)? = null

    fun onStart(start: () -> Unit) {
        this.start = start
    }

    fun onCompletion(completion: (successNum: Int, failNum: Int, urls: MutableList<String?>) -> Unit) {
        this.completion = completion
    }

    fun onFailure(fail: ((catchIndex: Int, errStr: String?) -> Unit)) {
        this.fail = fail
    }
}

@MainThread
fun UploadStateLiveData.singleUploadObserver(owner: LifecycleOwner?, observer: SingleUploadObserver.() -> Unit) {
    owner?.let { it ->
        val result = SingleUploadObserver();result.observer()
        observe(it, {
            when (it.currState) {
                UploadState.Start -> result.start?.invoke(it.index)
                UploadState.Progress -> result.progress?.invoke(it.index, it.progress, it.totalProgress)
                UploadState.Success -> result.success?.invoke(it.index, it.url, it.otherParams)
                UploadState.Fail -> result.fail?.invoke(it.index, it.errorCode, it.errMsg)
            }
        })
    }
}

@MainThread
fun MultipleUploadStateLiveData.multipleUploadObserver(owner: LifecycleOwner?, observer: MultipleUploadObserver.() -> Unit) {
    owner?.let { it ->
        val result = MultipleUploadObserver();result.observer()
        observe(it, {
            when (it.currState) {
                MultipleUploadState.Start -> result.start?.invoke()
                MultipleUploadState.Completion -> result.completion?.invoke(it.successNum, it.failNum, it.urls)
                MultipleUploadState.Fail -> result.fail?.invoke(it.catchIndex, it.errMsg)
            }
        })
    }
}

/**
 * 解决数据丢失问题
 */
fun UploadStateLiveData.postValueFix(handler: Handler, state: UploadState) {
    handler.post {
        setValue(state)
    }
}

/***状态接口，兼容java */
interface OnSingleUploadState {
    fun uploadState(state: UploadState)
}

/***状态接口，兼容java */
interface OnMultipleUploadState {
    fun uploadState(state: MultipleUploadState)
}