package com.lzx.multiple.impl

import androidx.lifecycle.LifecycleOwner
import com.lzx.multiple.MultipleUploadState
import com.lzx.multiple.MultipleUploadStateLiveData
import com.lzx.multiple.UploadState
import com.lzx.multiple.UploadStateLiveData
import com.lzx.multiple.upload.UploadCallback
import com.lzx.multiple.whatIfMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 多个上传逻辑
 */
class MultipleUploadManager(owner: LifecycleOwner?,
                            private val list: MutableList<String>,
                            private val params: HashMap<String, Any>
) : UploadBuilder(owner) {
    override fun asyncRun(scope: CoroutineScope?,
                          singleLiveData: UploadStateLiveData,
                          multipleLiveData: MultipleUploadStateLiveData) {
        if (uploadNut == null) return
        val uploadList = whatIfMap(filter != null, whatIf = {
            list.filter { filter?.apply(it) == true }
        }, whatIfNot = { list })
        if (uploadList.isNullOrEmpty()) return

        val multipleInfo = MultipleUploadState()
        val resultUrls = mutableListOf<String?>()
        var successNum = 0
        var failNum = 0
        var currIndex = 0

        scope?.launch {
            uploadList.mapIndexed { index, path ->
                async { uploadImpl(index, path, params) }
            }.asFlow().map {
                it.await()
            }.flowOn(supportDispatcher)
                .onStart {
                    multipleInfo.currState = MultipleUploadState.Start
                    multipleLiveData.value = multipleInfo
                }.onCompletion {
                    multipleInfo.currState = MultipleUploadState.Completion
                    multipleInfo.successNum = successNum
                    multipleInfo.failNum = failNum
                    multipleInfo.urls = resultUrls
                    multipleLiveData.value = multipleInfo
                    resultUrls.clear()
                    successNum = 0
                    failNum = 0
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
                        failNum++
                    }
                }
        }
    }

    private suspend fun uploadImpl(index: Int, path: String, params: HashMap<String, Any>) =
        suspendCoroutine<UploadState> { coroutine ->
            val uploadState = UploadState()
            uploadNut?.uploadFile(path, params, object : UploadCallback {
                override fun onUploadStart() {
                    uploadState.currState = UploadState.Start
                    uploadState.index = index
                    coroutine.resume(uploadState)
                }

                override fun onUploadProgress(progress: Int, totalProgress: Int) {
                    uploadState.currState = UploadState.Progress
                    uploadState.progress = progress
                    uploadState.totalProgress = totalProgress
                    uploadState.index = index
                    coroutine.resume(uploadState)
                }

                override fun onUploadSuccess(url: String) {
                    uploadState.currState = UploadState.Success
                    uploadState.url = url
                    uploadState.index = index
                    coroutine.resume(uploadState)
                }

                override fun onUploadFail(errCode: Int, errMsg: String?) {
                    uploadState.currState = UploadState.Fail
                    uploadState.errorCode = errCode
                    uploadState.errMsg = errMsg
                    uploadState.index = index
                    coroutine.resume(uploadState)
                }
            })
        }
}