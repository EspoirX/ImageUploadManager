package com.lzx.library;


import com.lzx.library.alpha.Task;

public class ImageUploadTask extends Task {
    private IUploadImageStrategy mStrategy;
    private UploadOptions mOptions;
    private String imagePath;
    private IUploadImageStrategy.OnUploadTaskListener mOnUploadTaskListener;

    ImageUploadTask(String name, int sequence,
                    IUploadImageStrategy strategy,
                    UploadOptions options, String imagePath,
                    IUploadImageStrategy.OnUploadTaskListener taskListener) {
        super(name, true, true, sequence);
        this.mStrategy = strategy;
        this.mOptions = options;
        this.imagePath = imagePath;
        mOnUploadTaskListener = taskListener;
    }

    @Override
    public void run() {

    }

    @Override
    public void runAsynchronous(final OnTaskAnsyListener listener) {
        mStrategy.uploadImageWithListener(mCurrTaskSequence, imagePath, mOptions,
                new IUploadImageStrategy.OnUploadTaskListener() {
                    @Override
                    public void onProcess(int sequence, long current, long total) {
                        mOnUploadTaskListener.onProcess(mCurrTaskSequence, current, total);
                    }

                    @Override
                    public void onFailure(int sequence, int errorCode, String errorMsg) {
                        listener.onTaskFinish(mName, "fail");
                        mOnUploadTaskListener.onFailure(mCurrTaskSequence, errorCode, errorMsg);
                    }

                    @Override
                    public void onSuccess(int sequence, String imageUrl) {
                        listener.onTaskFinish(mName, "success");
                        mOnUploadTaskListener.onSuccess(mCurrTaskSequence, imageUrl);
                    }
                });
    }
}
