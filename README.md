# ImageUploadManager

## 基于 Flow 的多线程并行通用图片上传框架，结合 LiveData 监听回调


### 特点：
1. 对外开放上传接口，具体上传逻辑自己定义
2. 基于 Kotlin 的 Flow 实现多线程并行上传，并且上传结果按顺序返回哦
3. 结合 LiveData 监听上传回调，管理生命周期
4. 提供拦截器功能，拦截器可在主线程和子线程中使用，提供过滤器功能

### API 预览：
```kotlin
MultipleUpload
    .with( .. )    //传入当前的 Lifecycle
    .load( .. )    //传入上传的路径，可传一个，可传多个
    .setUploadImpl( .. )    //配置自己具体的上传逻辑
    .setThreadPoolExecutor( .. )     //配置自定义的线程池，否则用默认的
    .filter { .. }         // 路径过滤器，dsl 形式
    .filter(object : UploadFilter { ..  })   // 路径过滤器，接口形式
    .addInterceptor(object : UploadIntercept() { .. }, UploadIntercept.UI) //添加拦截器，第二个参数是拦截器运行的线程。不传默认是UI
    .addInterceptor(object : UploadIntercept() { .. }, UploadIntercept.IO) //添加拦截器，运行在子线程
    .singleUploadObserver {        //单个上传的监听回调，dsl 形式
        onStart { index-> .. }     //开始上传，index 是当前上传的第几个文件
        onProgress { index, progress, totalProgress -> .. } //上传进度
        onSuccess { index, url, otherParams -> ..  }        //上传成功
        onFailure { index, errCode, errStr -> .. }          //上传失败
    }
    .singleUploadObserver(object : OnSingleUploadState{ .. })  //单个上传的监听回调，接口形式
    .multipleUploadObserver {         //多个文件上传时总体监听回调，dsl 形式
        onStart { .. }                //上传开始
        onCompletion { successNum, failNum, urls -> .. }   //全部上传完成
        onFailure { catchIndex, errStr -> .. }             //上传中发生 catch。
    }
    .multipleUploadObserver(object : OnMultipleUploadState{ .. }) //多个文件上传时总体监听回调，接口形式
    .upload()  //发起上传
```

### API详解

#### 1. with
```kotlin
fun with(context: Context)
fun with(activity: Activity)
fun with(context: FragmentActivity)
fun with(fragment: Fragment)
fun with(owner: LifecycleOwner = ProcessLifecycleOwner.get())
```
with 方法有多个重载，可以传入 context，activity 等，但 context 和 activity 都必须是 FragmentActivity 的子类，因为最终需要的是 LifecycleOwner。
如果不传，则默认是 ProcessLifecycleOwner.get()。

#### 2. load
```kotlin
fun load(path: String?)
fun load(list: MutableList<String?>)
fun load(path: String?, params: HashMap<String, Any>)
fun load(list: MutableList<String?>, params: HashMap<String, Any>)
```
load 方法有 4 个重载，可分成两类，一类是传入一个路径，即上传一个文件，一类是传入一个 List，即同时上传多个文件。

params 参数的意思是，在我们实现上传逻辑的时候，有时候不只需要一个 path，可能还需要其他一些业务参数，这时候就可以用到 params 来传递了。

#### 3. setUploadImpl
```kotlin
fun setUploadImpl(upload: UploadInterface)

interface UploadInterface {
    fun uploadFile(path: String, params: HashMap<String, Any>, callback: UploadCallback)
}

interface UploadCallback {
    fun onUploadStart()
    fun onUploadProgress(progress: Int, totalProgress: Int)
    fun onUploadSuccess(url: String, otherParams: HashMap<String, Any>? = null)
    fun onUploadFail(errCode: Int, errMsg: String?)
}
```
传入具体的上传逻辑，参数是 UploadInterface 接口，通过实现这个接口来实现自己具体的上传逻辑，然后通过 callback 回调给框架处理。
可以看到 uploadFile 方法的第二个参数就是上面 load 方法中说到的那个自定义参数了。

同时可以看到 onUploadSuccess 回调中第二个参数 otherParams，也是因为上传完成后可能需要传递一些其他业务内容出去，这时候也可以用到它去做。

#### 4. setThreadPoolExecutor
配置自定义的线程池，如果你想上传时的线程池由自己去做，可以通过它来传进去，参数是 ThreadPoolExecutor，不传的话会使用默认的。

#### 5. filter
```kotlin
fun filter(filter: UploadFilter)
fun filter(filter: (String) -> Boolean)

interface UploadFilter {
    fun apply(path: String): Boolean
}
```
filter 方法的作用是做一些路径过滤，需要实现 UploadFilter 接口，会过滤掉返回 false 的路径，它的调用时机是在上传前。

有两个重载，分别是接口形式和 dsl形式，可以理解为一个用于 kotlin，一个用于兼容 java。

随便一提，像判空，判断本地是否存在改文件这种过滤内部已经有了，就不需要自己去做了。


#### 6. addInterceptor
```kotlin
fun addInterceptor(interceptor: UploadIntercept,interceptThread: String = UploadIntercept.UI)

abstract class UploadIntercept {
    companion object {
        const val UI = "UI"
        const val IO = "IO"
    }

    open fun processSingle(path: String, callback: InterceptCallback) {}  //用于单个文件
    open fun processMultiple(paths: MutableList<String>, callback: InterceptCallback) {} //用于多个文件
}

interface InterceptCallback {
    fun onNext(path: String) //执行下一个，用于上传一个文件
    fun onNext(paths: MutableList<String>) //执行下一个，用于上传多个文件
    fun onInterrupt(code: Int = -1, msg: String?) //中断 code，msg:可以添加code 和 msg，如果 msg 不为空，则会回调失败回调
}
```
拦截器，作用大家应该很熟悉了，运行时机是在 filter 之后，上传之前。

实现拦截器需求实现 UploadIntercept，因为上传单个文件就一个路径，多个文件有多个路径，是一个 List，所以为了区分，就有了 processSingle 和 processMultiple 两个方法，可以根据需要实现。

InterceptCallback 回调接口， onNext 方法代表执行下一个拦截逻辑，同样分两个，作用也跟刚刚说的一样。

如果要中断逻辑，则可以调用 onInterrupt 方法，有两个参数，如果 msg 有值的话，会回调上传失败。

addInterceptor 添加拦截器的第二个参数 interceptThread，代表拦截器运行在 UI 还是 IO 线程，默认是 UI 线程。

#### 7. singleUploadObserver
```kotlin
fun singleUploadObserver(observer: SingleUploadObserver.() -> Unit)
fun singleUploadObserver(state: OnSingleUploadState)
```
单个文件上传监听，同样有两个重载，分别是接口形式和 dsl形式，可以理解为一个用于 kotlin，一个用于兼容 java。它是基于 LiveData 的。

有几个回调分别是 onStart，onProgress，onSuccess 和 onFailure。singleUploadObserver 无轮是上传多个还是一个文件的时候都会有回调。

它们都有一个参数 index 代表着目前正在上传第几个文件。onSuccess 中有一个参数是 otherParams，它的作用是上面第 3 点中 setUploadImpl 说到的。


#### 8. multipleUploadObserver
```kotlin
fun multipleUploadObserver(observer: MultipleUploadObserver.() -> Unit)
fun multipleUploadObserver(state: OnMultipleUploadState)
```
在多个文件上传时，除了需要了解每个文件上传的情况（即上面的 singleUploadObserver），还需要了解一些总体情况，比如上传开始，全部上传结束等，
multipleUploadObserver 就是这个作用，同样两个重载，接口形式和 dsl形式，用于 kotlin，一个用于兼容 java。

有三个回调是 onStart，onCompletion，和 onFailure，代表上传开始，全部上传结束和上传发生错误。

在 onCompletion 中，你可以拿到 successNum（上传文件成功数），failNum（上传文件失败数），urls（上传完成后文件路径集合，**注意这个集合的顺序是跟你传入上传路径那个集合顺序是一样的** ）

#### 9. upload
发起上传逻辑，调用这个方法后才会真正发起上传逻辑。