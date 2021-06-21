package com.lzx.multiple.impl

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import com.lzx.multiple.MultipleUploadState
import com.lzx.multiple.MultipleUploadStateLiveData
import com.lzx.multiple.UploadState
import com.lzx.multiple.UploadStateLiveData
import com.lzx.multiple.intercept.InterceptCallback
import com.lzx.multiple.intercept.UploadIntercept
import com.lzx.multiple.postValueFix
import com.lzx.multiple.upload.UploadCallback
import com.lzx.multiple.whatIfMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 多个上传逻辑
 */
class MultipleUploadManager(owner: LifecycleOwner?,
                            private val list: MutableList<String>,
                            private val params: HashMap<String, Any>
) : UploadBuilder(owner) {

    private val handler = Handler(Looper.getMainLooper())

    override fun asyncRun(scope: CoroutineScope?,
                          singleLiveData: UploadStateLiveData,
                          multipleLiveData: MultipleUploadStateLiveData) {
        if (uploadNut == null) return
        val uploadList = whatIfMap(filter != null, whatIf = {
            list.filter { filter?.apply(it) == true }
        }, whatIfNot = { list })
        if (uploadList.isNullOrEmpty()) return

        scope?.launch {
            if (interceptors.isEmpty()) {
                multipleUpload(this, uploadList, singleLiveData, multipleLiveData)
            } else {
                doInterceptor(0, this, uploadList, singleLiveData, multipleLiveData)
            }
        }
    }

    /**
     * 拦截器逻辑
     */
    private suspend fun doInterceptor(index: Int,
                                      scope: CoroutineScope,
                                      uploadList: List<String>,
                                      singleLiveData: UploadStateLiveData,
                                      multipleLiveData: MultipleUploadStateLiveData) {
        if (index < interceptors.size) {
            val triple = interceptors[index]
            val interceptor = triple.first
            val interceptThread = triple.second
            if (interceptThread == UploadIntercept.UI) {
                withContext(Dispatchers.Main) {
                    doInterceptImpl(interceptor, index, scope, uploadList, singleLiveData, multipleLiveData)
                }
            } else {
                withContext(supportDispatcher) {
                    doInterceptImpl(interceptor, index, scope, uploadList, singleLiveData, multipleLiveData)
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                multipleUpload(scope, uploadList, singleLiveData, multipleLiveData)
            }
        }
    }

    private fun doInterceptImpl(interceptor: UploadIntercept,
                                index: Int,
                                scope: CoroutineScope?,
                                uploadList: List<String>,
                                singleLiveData: UploadStateLiveData,
                                multipleLiveData: MultipleUploadStateLiveData) {
        interceptor.processMultiple(list, object : InterceptCallback {
            override fun onNext(path: String) {
            }

            override fun onNext(paths: MutableList<String>) {
                scope?.launch {
                    doInterceptor(index + 1, scope, uploadList, singleLiveData, multipleLiveData)  //执行下一个
                }
            }

            override fun onInterrupt(code: Int, msg: String?) {
                msg?.let {
                    val multipleInfo = MultipleUploadState()
                    multipleInfo.currState = MultipleUploadState.Fail
                    multipleInfo.errMsg = it
                    multipleLiveData.postValue(multipleInfo)
                }
            }
        })
    }

    /**
     * 多线程逻辑
     */
    private suspend fun multipleUpload(scope: CoroutineScope,
                                       uploadList: List<String>,
                                       singleLiveData: UploadStateLiveData,
                                       multipleLiveData: MultipleUploadStateLiveData) {
        val multipleInfo = MultipleUploadState()
        val resultUrls = mutableListOf<String?>()
        var successNum = 0
        var failNum = uploadList.size
        var currIndex = 0
        uploadList.mapIndexed { index, path ->
            scope.async(supportDispatcher) {
                uploadImpl(index, path, params, singleLiveData)
            }
        }.asFlow().map {
            it.await()
        }.flowOn(supportDispatcher)
            .onStart {
                multipleInfo.currState = MultipleUploadState.Start
                multipleLiveData.value = multipleInfo
            }.onCompletion {
                multipleInfo.currState = MultipleUploadState.Completion
                multipleInfo.successNum = successNum
                multipleInfo.failNum = whatIfMap(failNum == uploadList.size, whatIf = { 0 }, whatIfNot = { failNum })
                multipleInfo.urls = resultUrls
                multipleLiveData.value = multipleInfo
                resultUrls.clear()
                successNum = 0
                failNum = uploadList.size
                currIndex = 0
            }.catch {
                multipleInfo.currState = MultipleUploadState.Fail
                multipleInfo.errMsg = it.message
                multipleInfo.catchIndex = currIndex
                multipleLiveData.value = multipleInfo
            }.collect {
                resultUrls.add(it.url)
                currIndex = it.index
                if (it.currState == UploadState.Success) {
                    successNum++
                } else if (it.currState == UploadState.Fail) {
                    failNum--
                }
            }
    }

    /**
     * 具体上传逻辑
     */
    private suspend fun uploadImpl(index: Int, path: String,
                                   params: HashMap<String, Any>,
                                   singleLiveData: UploadStateLiveData): UploadState =
        suspendCoroutine { coroutine ->
            val uploadState = UploadState()
            uploadNut?.uploadFile(path, params, object : UploadCallback {
                override fun onUploadStart() {
                    uploadState.currState = UploadState.Start
                    uploadState.index = index
                    singleLiveData.postValueFix(handler, uploadState)
                }

                override fun onUploadProgress(progress: Int, totalProgress: Int) {
                    uploadState.currState = UploadState.Progress
                    uploadState.progress = progress
                    uploadState.totalProgress = totalProgress
                    uploadState.index = index
                    singleLiveData.postValueFix(handler, uploadState)
                }

                override fun onUploadSuccess(url: String, otherParams: HashMap<String, Any>?) {
                    uploadState.currState = UploadState.Success
                    uploadState.url = url
                    uploadState.otherParams = otherParams
                    uploadState.index = index
                    singleLiveData.postValueFix(handler, uploadState)
                    coroutine.resume(uploadState)
                }

                override fun onUploadFail(errCode: Int, errMsg: String?) {
                    uploadState.currState = UploadState.Fail
                    uploadState.errorCode = errCode
                    uploadState.errMsg = errMsg
                    uploadState.index = index
                    singleLiveData.postValueFix(handler, uploadState)
                    coroutine.resume(uploadState)
                }
            })
        }
}