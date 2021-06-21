package com.lzx.multiple.upload

interface UploadInterface {
    fun uploadFile(path: String, params: HashMap<String, Any>, callback: UploadCallback)
}

interface UploadCallback {
    fun onUploadStart()
    fun onUploadProgress(progress: Int, totalProgress: Int)
    fun onUploadSuccess(url: String)
    fun onUploadFail(errCode: Int, errMsg: String?)
}