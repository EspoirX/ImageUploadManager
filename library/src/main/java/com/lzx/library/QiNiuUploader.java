package com.lzx.library;

import android.text.TextUtils;

/**
 * 七牛上传
 */
public class QiNiuUploader implements IUploadImageStrategy {

    private static final String TAG = "QiNiuUploader";

    QiNiuUploader() {

    }

    @Override
    public void uploadImageWithListener(int sequence, String imagePath, UploadOptions options,
                                        OnUploadTaskListener taskListener) {
        if (TextUtils.isEmpty(imagePath)) {
            return;
        }
        //这里是上传的具体代码，就不写了，上传完通过回调把结果回调出去。。。
        if (taskListener != null) {
            taskListener.onSuccess(sequence, "http://p3.pstatp.com/large/6c2a0008d4bf2b6df897");
        }
    }

}
