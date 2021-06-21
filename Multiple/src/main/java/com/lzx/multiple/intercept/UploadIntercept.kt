package com.lzx.multiple.intercept

abstract class UploadIntercept {
    companion object {
        const val UI = "UI"
        const val IO = "IO"
    }

    open fun processSingle(path: String, callback: InterceptCallback) {}

    open fun processMultiple(paths: MutableList<String>, callback: InterceptCallback) {}
}

interface InterceptCallback {
    /**
     * 执行下一个，用于上传一个文件
     */
    fun onNext(path: String)

    /**
     * 执行下一个，用于上传多个文件
     */
    fun onNext(paths: MutableList<String>)

    /**
     * 中断
     * code，msg:可以添加code 和 msg，如果 msg 不为空，则会回调失败回调
     */
    fun onInterrupt(code: Int = -1, msg: String?)
}

