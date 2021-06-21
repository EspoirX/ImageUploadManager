package com.lzx.multiple.impl

import androidx.lifecycle.LifecycleOwner
import com.lzx.multiple.MultipleUploadStateLiveData
import com.lzx.multiple.UploadState
import com.lzx.multiple.UploadStateLiveData
import com.lzx.multiple.upload.UploadCallback
import kotlinx.coroutines.CoroutineScope

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

            override fun onUploadSuccess(url: String) {
                uploadState.currState = UploadState.Success
                uploadState.url = url
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