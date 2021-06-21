package com.lzx.multiple.impl

import androidx.lifecycle.LifecycleOwner
import com.lzx.multiple.MultipleUploadStateLiveData
import com.lzx.multiple.UploadState
import com.lzx.multiple.UploadStateLiveData
import com.lzx.multiple.intercept.InterceptCallback
import com.lzx.multiple.intercept.UploadIntercept
import com.lzx.multiple.upload.UploadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 单个上传逻辑
 */
class SingleUploadManager(owner: LifecycleOwner?,
                          private val path: String,
                          private val params: HashMap<String, Any>
) : UploadBuilder(owner) {


    override fun asyncRun(scope: CoroutineScope?,
                          singleLiveData: UploadStateLiveData,
                          multipleLiveData: MultipleUploadStateLiveData) {
        if (filter?.apply(path) == false) return
        if (uploadNut == null) return

        if (interceptors.isEmpty()) {
            uploadImpl(singleLiveData)
        } else {
            scope?.launch {
                doInterceptor(0, scope, singleLiveData)
            }
        }
    }

    private suspend fun doInterceptor(index: Int, scope: CoroutineScope?, singleLiveData: UploadStateLiveData) {
        if (index < interceptors.size) {
            val pair = interceptors[index]
            val interceptor = pair.first
            val interceptThread = pair.second
            if (interceptThread == UploadIntercept.UI) {
                withContext(Dispatchers.Main) {
                    doInterceptImpl(interceptor, index, scope, singleLiveData)
                }
            } else {
                withContext(supportDispatcher) {
                    doInterceptImpl(interceptor, index, scope, singleLiveData)
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                uploadImpl(singleLiveData)
            }
        }
    }

    private fun doInterceptImpl(interceptor: UploadIntercept,
                                index: Int,
                                scope: CoroutineScope?,
                                singleLiveData: UploadStateLiveData) {
        interceptor.processSingle(path, object : InterceptCallback {
            override fun onNext(path: String) {
                scope?.launch {
                    doInterceptor(index + 1, scope, singleLiveData)  //执行下一个
                }
            }

            override fun onNext(paths: MutableList<String>) {
            }

            override fun onInterrupt(code: Int, msg: String?) {
                msg?.let {
                    val uploadState = UploadState()
                    uploadState.currState = UploadState.Fail
                    uploadState.errorCode = code
                    uploadState.errMsg = msg
                    singleLiveData.postValue(uploadState)
                }
            }
        })
    }

    private fun uploadImpl(singleLiveData: UploadStateLiveData) {
        val uploadState = UploadState()
        uploadNut?.uploadFile(path, params, object : UploadCallback {
            override fun onUploadStart() {
                uploadState.currState = UploadState.Start
                singleLiveData.value = uploadState
            }

            override fun onUploadProgress(progress: Int, totalProgress: Int) {
                uploadState.currState = UploadState.Progress
                uploadState.progress = progress
                uploadState.totalProgress = totalProgress
                singleLiveData.value = uploadState
            }

            override fun onUploadSuccess(url: String, otherParams: HashMap<String, Any>?) {
                uploadState.currState = UploadState.Success
                uploadState.url = url
                uploadState.otherParams = otherParams
                singleLiveData.value = uploadState
            }

            override fun onUploadFail(errCode: Int, errMsg: String?) {
                uploadState.currState = UploadState.Fail
                uploadState.errorCode = errCode
                uploadState.errMsg = errMsg
                singleLiveData.value = uploadState
            }
        })
    }
}