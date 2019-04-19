# ImageUploadManager

## 利用 策略模式 结合 alibaba/alpha 框架优化你的图片上传功能

### 特点：
1. 可以一键修改上传方式，不影响代码业务逻辑
2. 上传多张图片时，所有任务同步执行，最后再汇总。所以整个上传时间基本等于 N 张图片中单张上传用时最久的那个时间。
3. 你可以知道每个上传任务完成的用时，全部上传任务的总用时，还有每个任务的状态以及进度，每个任务还可以随你选择在主线程还是子线程去完成。
4. 具体的上传逻辑与上传时配置的参数还有上传管理三个部分都分离开来，解耦合，方便维护和扩展

###如何使用：
1. 实现 IUploadImageStrategy 方法，实现自己的上传具体代码
2. 在 Application 或者在 ImageUploadManager 的构造方法里面调用 setGlobalImageLoader 方法把你的上传方法设置进去
3. 根据自己实际的需求在 UploadOptions 里面添加或删除一些上传参数，当然其他类也可以根据自己的实际需要做修改
4. 检查一切都没什么问题后就可以愉快的上传了


使用起来的效果：

```java
List<String> paths = new ArrayList<>();
paths.add("你的上传path");
paths.add("你的上传path");
paths.add("你的上传path");
paths.add("你的上传path");

ImageUploadManager.getInstance().uploadImage(paths)
        .setShowProgress(true)
        .setContext(this)
        .uploadListener(new IUploadListener() {
            @Override
            public void onProcess(int sequence, long current, long total) {
                Log.i("MainActivity", "第 " + sequence + "张图片上传进度 current = "
                        + current + " total = " + total);
            }

            @Override
            public void onTotalProcess(long current, long total) {
                Log.i("MainActivity", "总上传进度 current = " + current + " total = " + total);
            }

            @Override
            public void onFailure(int sequence, int errorCode, String errorMsg) {
                Log.i("MainActivity", "第 " + sequence + " 张图上传失败，errorMsg = " + errorMsg);
            }

            @Override
            public void onSuccess(int sequence, String imageUrl) {
                Log.i("MainActivity", "第 " + sequence + " 张图上传成功，url = " + imageUrl);
            }

            @Override
            public void onTotalSuccess(int successNum, int failNum, int totalNum) {
                Log.i("MainActivity", "全部上传完成，成功数量 = " + successNum
                        + " 失败数量 = " + failNum + " 总数 = " + totalNum);
            }
        }).go();
```