package com.lzx.library;

public interface IUploadImageStrategy {

    void uploadImageWithListener(int sequence, String imagePath, UploadOptions options,
                                 OnUploadTaskListener taskListener);

    interface OnUploadTaskListener {
        void onProcess(int sequence, long current, long total); //每一张上传进度

        void onFailure(int sequence, int errorCode, String errorMsg); //每一张的上传失败回调

        void onSuccess(int sequence, String imageUrl); //每一张的上传成功回调
    }
}
