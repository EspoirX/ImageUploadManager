package com.lzx.library;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * 上传参数配置
 */
public class UploadOptions {
    Context mContext; //需要弹窗提示窗的时候要设置
    private boolean isSingle;
    boolean isShowProgress; //是否展示进度提示
    IUploadListener mUploadListener; //回调
    private ProgressDialog mProgressDialog;
    IUploadImageStrategy mStrategy;


    public void go() {
        if (isSingle) {
            //单张图片
            ImageUploadManager.getInstance().loadOptionsAtOneImage(this);
        } else {
            //多张图片
            ImageUploadManager.getInstance().loadOptionsAtMoreImage(this);
        }
    }

    public UploadOptions loader(IUploadImageStrategy mStrategy) {
        this.mStrategy = mStrategy;
        return this;
    }

    public UploadOptions setContext(Context context) {
        mContext = context;
        return this;
    }

    public UploadOptions setShowProgress(boolean showProgress) {
        isShowProgress = showProgress;
        if (mContext != null) {
            ImageUploadManager.sMainThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    createProgressDialog();
                }
            });
        }
        return this;
    }

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(mContext);
    }

    public UploadOptions uploadListener(IUploadListener uploadListener) {
        mUploadListener = uploadListener;
        return this;
    }

    void showProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    boolean isSingle() {
        return isSingle;
    }

    void setSingle(boolean single) {
        isSingle = single;
    }
}
