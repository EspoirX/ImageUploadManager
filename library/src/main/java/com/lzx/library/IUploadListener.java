package com.lzx.library;

public interface IUploadListener {
    void onProcess(int sequence, long current, long total); //每一张上传进度

    void onTotalProcess(long current, long total);  //多张图片时总的上传进度

    void onFailure(int sequence, int errorCode, String errorMsg); //每一张的上传失败回调

    void onSuccess(int sequence, String imageUrl); //每一张的上传成功回调

    void onTotalSuccess(int successNum, int failNum, int totalNum); //多张图片时总的上传成功回调

    abstract class SimpleUploadListener implements IUploadListener {

        @Override
        public void onProcess(int sequence, long current, long total) {

        }

        @Override
        public void onFailure(int sequence, int errorCode, String errorMsg) {

        }

        @Override
        public void onSuccess(int sequence, String imageUrl) {

        }

        @Override
        public void onTotalSuccess(int successNum, int failNum, int totalNum) {

        }

        @Override
        public void onTotalProcess(long current, long total) {

        }
    }
}
