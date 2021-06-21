package com.lzx.multiple.upload

interface UploadInterface {
    fun uploadFile(path: String, params: HashMap<String, Any>, callback: UploadCallback)
}

interface UploadCallback {
    fun onUploadStart()
    fun onUploadProgress(progress: Int, totalProgress: Int)

    //otherParams:上传成功后可能有一些其他的业务信息,通过这个传递出去
    fun onUploadSuccess(url: String, otherParams: HashMap<String, Any>? = null)
    fun onUploadFail(errCode: Int, errMsg: String?)
}