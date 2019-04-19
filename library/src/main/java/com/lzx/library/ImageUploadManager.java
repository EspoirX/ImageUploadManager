package com.lzx.library;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;


import com.lzx.library.alpha.AlphaManager;
import com.lzx.library.alpha.Project;
import com.lzx.library.alpha.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 上传图片管理类，单张图片直接上传，多张图片扔到PERT图中上传
 */
public class ImageUploadManager {
    //单例模式
    private static volatile ImageUploadManager sInstance;
    //上传接口，里面实现了具体的上传方法
    private static IUploadImageStrategy sStrategy;
    //主线程,保证在子线程中调用也没事
    static final Executor sMainThreadExecutor = new MainThreadExecutor();
    //多张图片的 url List
    private List<String> imagePaths = new ArrayList<>();
    //单张图片的图片 url
    private String imagePath;

    private boolean isUploadQiNiu = true;

    private ImageUploadManager() {
        //可以看到，这里通过策略模式可以实现一键切换上传方案，不影响具体业务逻辑
        if (isUploadQiNiu) {
            setGlobalImageLoader(new QiNiuUploader());
        } else {
            setGlobalImageLoader(new OtherUploader());
        }
    }

    //设置上传方式
    public void setGlobalImageLoader(IUploadImageStrategy strategy) {
        sStrategy = strategy;
    }

    //单例模式
    public static ImageUploadManager getInstance() {
        if (sInstance == null) {
            synchronized (ImageUploadManager.class) {
                if (sInstance == null) {
                    sInstance = new ImageUploadManager();
                }
            }
        }
        return sInstance;
    }

    //上传图片方法，单张图片
    public UploadOptions uploadImage(String imagePath) {
        this.imagePath = imagePath;
        UploadOptions options = new UploadOptions();
        options.setSingle(true); //设置标记位
        return options;
    }

    //上传图片方法，多张图片
    public UploadOptions uploadImage(List<String> imagePaths) {
        this.imagePaths = imagePaths;
        UploadOptions options = new UploadOptions();
        options.setSingle(false); //设置标记位
        return options;
    }

    /**
     * 单张图片上传 被 UploadOptions中的 go() 方法调用
     */
    void loadOptionsAtOneImage(final UploadOptions options) {
        sMainThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                setUploadImageAtOneImage(options);
            }
        });
    }

    /**
     * 多张图片上传 被 UploadOptions中的 go() 方法调用
     */
    void loadOptionsAtMoreImage(final UploadOptions options) {
        sMainThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                setUploadImageAtMoreImage(options);
            }
        });
    }

    //单张图片上传具体实现
    private void setUploadImageAtOneImage(UploadOptions options) {
        if (options.mStrategy != null) {
            checkShowProgressDialog(options);
            options.mStrategy.uploadImageWithListener(0, imagePath, options, new UploadTaskListener(options));
        } else {
            checkStrategyNotNull();
            checkShowProgressDialog(options);
            sStrategy.uploadImageWithListener(0, imagePath, options, new UploadTaskListener(options));
        }
    }

    /**
     * 具体上传回调
     */
    private static class UploadTaskListener implements IUploadImageStrategy.OnUploadTaskListener {
        UploadOptions options;

        UploadTaskListener(UploadOptions options) {
            this.options = options;
        }

        @Override
        public void onProcess(final int sequence, final long current, final long total) {
            sMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (options.mUploadListener != null) {
                        options.mUploadListener.onProcess(sequence, current, total);
                        //当上传一张图片的时候，也把 onTotalProcess 设置一下
                        if (options.isSingle()) {
                            options.mUploadListener.onTotalProcess(current, total);
                        }
                    }
                }
            });
        }

        @Override
        public void onFailure(final int sequence, final int errorCode, final String errorMsg) {
            sMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    //先取消掉提示框
                    if (options.isSingle() && options.isShowProgress) {
                        options.dismissProgressDialog();
                    }
                    if (options.mUploadListener != null) {
                        options.mUploadListener.onFailure(sequence, errorCode, errorMsg);
                        //当上传一张图片的时候，回调一下上传完成方法，但是成功数量为 0
                        if (options.isSingle()) {
                            options.mUploadListener.onTotalSuccess(0, 1, 1);
                        }
                    }
                }
            });
        }

        @Override
        public void onSuccess(final int sequence, final String imageUrl) {
            sMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    //先取消掉提示框
                    if (options.isSingle() && options.isShowProgress) {
                        options.dismissProgressDialog();
                    }
                    if (options.mUploadListener != null) {
                        options.mUploadListener.onSuccess(sequence, imageUrl);
                        //当上传一张图片的时候，回调一下上传完成方法，成功数量为 1
                        if (options.isSingle()) {
                            options.mUploadListener.onTotalSuccess(1, 0, 1);
                        }
                    }
                }
            });
        }
    }

    //多张图片时的：
    private int successNum; //上传成功数量
    private int failNum;    //上传失败数量
    private int totalNum;   //上传总数
    private int currentIndex; //当前上传到第几张（从0开始）

    /**
     * 利用PERT图结构（总分总）上传，图片上传耗时 约等于 所有图片中耗时最长的那张图片的时间
     */
    private void setUploadImageAtMoreImage(final UploadOptions options) {
        //检查 sStrategy
        IUploadImageStrategy strategy;
        if (options.mStrategy != null) {
            strategy = options.mStrategy;
        } else {
            checkStrategyNotNull();
            strategy = sStrategy;
        }
        //初始化变量
        successNum = 0;
        failNum = 0;
        currentIndex = 0;
        totalNum = imagePaths.size();
        //检查是否需要弹出提示框
        checkShowProgressDialog(options);
        //创建一个空的PERT头
        EmptyTask firstTask = new EmptyTask();
        Project.Builder builder = new Project.Builder();
        builder.add(firstTask); //添加一个耗时基本为0的紧前
        //循环添加任务到alpha中，任务名是 url 的 md5 值，任务序号是 i
        for (int i = 0; i < imagePaths.size(); i++) {
            //添加上传任务 Task
            ImageUploadTask task = new ImageUploadTask(
                    MD5.hexdigest(imagePaths.get(i)),
                    i, strategy, options, imagePaths.get(i),
                    new UploadTaskListener(options));
            //每个 task 添加执行完成回调，里面做数量的计算
            task.addOnTaskFinishListener(new Task.OnTaskFinishListener() {
                @Override
                public void onTaskFinish(String taskName, int currTaskSequence, String taskStatus) {
                    if ("success".equals(taskStatus)) {
                        successNum++;
                    } else {
                        failNum++;
                    }
                    currentIndex++;
                    if (options.mUploadListener != null) {
                        options.mUploadListener.onTotalProcess((currentIndex / totalNum) * 100, 100);
                    }
                }
            });
            builder.add(task).after(firstTask); //其他任务全部为紧后，同步执行
        }
        Project project = builder.create();
        //添加全部 task 上传完时的回调
        project.addOnTaskFinishListener(new Task.OnTaskFinishListener() {
            @Override
            public void onTaskFinish(String taskName, int currTaskSequence, String taskStatus) {
                if (options.isShowProgress) {
                    options.dismissProgressDialog();
                }
                if (options.mUploadListener != null) {
                    options.mUploadListener.onTotalSuccess(successNum, failNum, totalNum);
                }
            }
        });
        AlphaManager.getInstance(options.mContext).addProject(project);
        //开始上传
        AlphaManager.getInstance(options.mContext).start();
    }

    private static class EmptyTask extends Task {

        EmptyTask() {
            super("EmptyTask");
        }

        @Override
        public void run() {

        }

        @Override
        public void runAsynchronous(OnTaskAnsyListener listener) {

        }
    }

    //检查一下是否需要弹出上传提提示框
    private void checkShowProgressDialog(UploadOptions options) {
        if (options.isShowProgress) {
            options.showProgressDialog();
        }
    }

    //检查一下 sStrategy 是否为 null
    private void checkStrategyNotNull() {
        if (sStrategy == null) {
            throw new NullPointerException("you must be set your IUploadImageStrategy at first!");
        }
    }

    //主线程，如果当前为主线程，则直接执行，否则切到主线程执行
    private static class MainThreadExecutor implements Executor {
        final Handler mHandler = new Handler(Looper.getMainLooper());

        MainThreadExecutor() {
        }

        public void execute(@NonNull Runnable command) {
            if (checkIsMainThread()) {
                command.run();
            } else {
                this.mHandler.post(command);
            }
        }
    }

    private static boolean checkIsMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
