package com.lzx.imageupload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.lzx.library.IUploadListener;
import com.lzx.library.ImageUploadManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 调用例子
         */
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
    }
}
