package com.lzx.imageupload

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzx.multiple.MultipleUpload
import com.lzx.multiple.intercept.InterceptCallback
import com.lzx.multiple.intercept.UploadIntercept
import com.lzx.multiple.upload.UploadCallback
import com.lzx.multiple.upload.UploadInterface
import com.qw.soul.permission.SoulPermission
import com.qw.soul.permission.bean.Permission
import com.qw.soul.permission.bean.Permissions
import com.qw.soul.permission.callbcak.CheckRequestPermissionsListener

class MainActivity : AppCompatActivity() {

    private var resultText: TextView? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultText = findViewById(R.id.resultText)

        SoulPermission.getInstance().checkAndRequestPermissions(Permissions.build(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : CheckRequestPermissionsListener {
                override fun onAllPermissionOk(allPermissions: Array<Permission>) {
                }

                override fun onPermissionDenied(refusedPermissions: Array<Permission>) {
                }
            })

        findViewById<View>(R.id.btn1).setOnClickListener {
            MultipleUpload
                .with(this@MainActivity)
                .load("0.jpg".toSdcardPath())
//                .filter(object : UploadFilter {
//                    override fun apply(path: String): Boolean {
//                        return path.contains("IMG_20210621_145921")
//                    }
//                })
                .filter { it.contains("IMG_20210621_145921") }
                .addInterceptor(object : UploadIntercept() {
                    override fun processSingle(path: String, callback: InterceptCallback) {
                        super.processSingle(path, callback)
                        Log.i("MainActivity", "执行拦截器 path = $path thread = " + Thread.currentThread().name)
                        callback.onNext(path)
                    }
                }, UploadIntercept.IO)
                .addInterceptor(object : UploadIntercept() {
                    override fun processSingle(path: String, callback: InterceptCallback) {
                        super.processSingle(path, callback)
                        Log.i("MainActivity", "执行拦截器2 path = $path thread = " + Thread.currentThread().name)
                        callback.onNext(path)
                    }
                }, UploadIntercept.UI)
                .addInterceptor(object : UploadIntercept() {
                    override fun processSingle(path: String, callback: InterceptCallback) {
                        super.processSingle(path, callback)
                        Log.i("MainActivity", "执行拦截器3 path = $path thread = " + Thread.currentThread().name)
                        callback.onNext(path)
                    }
                }, UploadIntercept.IO)
                .setUploadImpl(TestUpload())
                .singleUploadObserver {
                    onStart {
                        Log.i("MainActivity", "上传开始...")
                        resultText?.text = "上传开始..."
                    }
                    onProgress { _, progress, totalProgress ->
                        Log.i("MainActivity", "上传中 progress = $progress totalProgress = $totalProgress")
                        resultText?.text = "上传中 progress = $progress totalProgress = $totalProgress"
                    }
                    onSuccess { _, url, _ ->
                        Log.i("MainActivity", "上传成功 url = $url")
                        resultText?.text = "上传成功, url = $url"
                    }
                    onFailure { _, errCode, errStr ->
                        Log.i("MainActivity", "上传失败 errCode = $errCode errStr = $errStr")
                        resultText?.text = "上传失败 errCode = $errCode errStr = $errStr"
                    }
                }
                .upload()
        }



        findViewById<View>(R.id.btn2).setOnClickListener {
            var startTime: Long = 0
            val paths = mutableListOf<String?>()
            paths.add("0.jpg".toSdcardPath())
            paths.add("1.jpg".toSdcardPath())
            paths.add("2.jpg".toSdcardPath())
            MultipleUpload
                .with(this@MainActivity)
                .load(paths)
                .setUploadImpl(TestUpload())
                .addInterceptor(object : UploadIntercept() {
                    override fun processMultiple(paths: MutableList<String>, callback: InterceptCallback) {
                        super.processMultiple(paths, callback)
                        Log.i("MainActivity", "执行拦截器 path = $paths thread = " + Thread.currentThread().name)
                        callback.onNext(paths)
                    }
                }, UploadIntercept.IO)
                .addInterceptor(object : UploadIntercept() {
                    override fun processMultiple(paths: MutableList<String>, callback: InterceptCallback) {
                        super.processMultiple(paths, callback)
                        Log.i("MainActivity", "执行拦截器2 path = $paths thread = " + Thread.currentThread().name)
                        callback.onNext(paths)
                    }
                }, UploadIntercept.UI)
                .addInterceptor(object : UploadIntercept() {
                    override fun processMultiple(paths: MutableList<String>, callback: InterceptCallback) {
                        super.processMultiple(paths, callback)
                        Log.i("MainActivity", "执行拦截器3 path = $paths thread = " + Thread.currentThread().name)
                        callback.onNext(paths)
                    }
                }, UploadIntercept.IO)
                .singleUploadObserver {
                    onStart {
                        Log.i("MainActivity", "第 $it 个文件上传开始...")
                        resultText?.text = "第 $it 个文件上传开始..."
                    }
                    onProgress { index, progress, totalProgress ->
                        Log.i("MainActivity", "第 $index 个文件上传中 progress = $progress totalProgress = $totalProgress")
                        resultText?.text = "第 $index 个文件上传中 progress = $progress totalProgress = $totalProgress"
                    }
                    onSuccess { index, url, _ ->
                        Log.i("MainActivity", "第 $index 个文件上传成功 url = $url")
                        resultText?.text = "第 $index 个文件上传成功 url = $url"
                    }
                    onFailure { index, errCode, errStr ->
                        Log.i("MainActivity", "第 $index 个文件上传失败 errCode = $errCode errStr = $errStr")
                        resultText?.text = "第 $index 个文件上传失败 errCode = $errCode errStr = $errStr"
                    }
                }
                .multipleUploadObserver {
                    onStart {
                        startTime = System.currentTimeMillis()
                        Log.i("MainActivity", "多文件上传开始...")
                        resultText?.text = "多文件上传开始..."
                    }
                    onCompletion { successNum, failNum, urls ->
                        Log.i("MainActivity", "耗时 = " + (System.currentTimeMillis() - startTime))
                        Log.i("MainActivity", "多文件上传结束 成功数 = $successNum 失败数 = $failNum 上传成功集合 = $urls")
                        resultText?.text = "多文件上传结束 成功数 = $successNum 失败数 = $failNum 上传成功集合 = $urls"
                    }
                    onFailure { catchIndex, errStr ->
                        Log.i("MainActivity", "多文件上传第 $catchIndex 个文件发生错误 ，errStr = $errStr")
                        resultText?.text = "多文件上传第 $catchIndex 个文件发生错误 ，errStr = $errStr"
                    }
                }
                .upload()
        }
    }
}


/**
 * 模拟上传
 */
class TestUpload : UploadInterface {


    override fun uploadFile(path: String, params: HashMap<String, Any>, callback: UploadCallback) {
        val xxUpload = XXUpload(path)
        xxUpload.callback = object : XXUpload.Callback {
            override fun onStart() {
                callback.onUploadStart()
            }

            override fun onPro(pro: Int, total: Int) {
                callback.onUploadProgress(pro, total)
            }

            override fun onSuccess(url: String) {
                callback.onUploadSuccess(url)
            }
        }
        xxUpload.startUpload()
    }
}

fun String.toSdcardPath(): String {
    return Environment.getExternalStorageDirectory().absolutePath.toString() + "/" + this
}

class XXUpload(private val path: String) {

    private var pro = 0
    private var totalPro = 1
    var callback: Callback? = null

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (pro <= totalPro) {
                callback?.onPro(pro, totalPro)
                pro++
                sendMsg()
            } else {
                val url = path.substring(path.length - 5, path.length)
                callback?.onSuccess("我是成功的url = " + url)
            }
        }
    }

    private fun sendMsg() {
        handler.sendEmptyMessageDelayed(0, 1000)
    }

    fun startUpload() {
        callback?.onStart()
        sendMsg()
    }

    interface Callback {
        fun onStart()
        fun onPro(pro: Int, total: Int)
        fun onSuccess(url: String)
    }
}