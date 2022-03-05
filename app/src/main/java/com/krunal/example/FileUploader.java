package com.krunal.example;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class FileUploader  {

    private FileUploaderCallback mFileUploaderCallback;
    long filesize = 0l;

    FileUploader(File file, Context context) {
        filesize = file.length();

        InterfaceFileUpload interfaceFileUpload = ApiClient.getRetrofitInstance()
                .create(InterfaceFileUpload.class);

        PRRequestBody mFile = new PRRequestBody(file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file",
                file.getName(), mFile);
        Call<UploadResponse> fileUpload = interfaceFileUpload.UploadFile(fileToUpload);

        fileUpload.enqueue(new Callback<UploadResponse>() {

            @Override
            public void onResponse(@NonNull Call<UploadResponse> call,
                                   @NonNull Response<UploadResponse> response) {

                if (response != null && response.code() == 200) {
//                    if (response.body().getSuccess().equalsIgnoreCase("1")) {
                        mFileUploaderCallback.onFinish(response.body().getFileDownloadUri());
                        Toast.makeText(context, response.body().getFileDownloadUri(), Toast.LENGTH_LONG).show();
//                    }

                }


            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                mFileUploaderCallback.onError();
            }
        });


    }

    public void SetCallBack(FileUploaderCallback fileUploaderCallback) {
        this.mFileUploaderCallback = fileUploaderCallback;
    }


    public class PRRequestBody extends RequestBody {
        private File mFile;

        private static final int DEFAULT_BUFFER_SIZE = 20000048;

        public PRRequestBody(final File file) {
            mFile = file;
        }

        @Override
        public MediaType contentType() {
            // i want to upload only images
            return MediaType.parse("multipart/form-data");
        }

        @Override
        public long contentLength() throws IOException {
            return mFile.length();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            long fileLength = mFile.length();
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            FileInputStream in = new FileInputStream(mFile);
            long uploaded = 0;
//            Source source = null;

            try {
                int read;
//                source = Okio.source(mFile);
                Handler handler = new Handler(Looper.getMainLooper());
                while ((read = in.read(buffer)) != -1) {

                    // update progress on UI thread
                    handler.post(new ProgressUpdater(uploaded, fileLength));
                    uploaded += read;
                    sink.write(buffer, 0, read);
                    Log.e("FileUploader", String.valueOf(uploaded));

                }



//                sink.writeAll(source);
            } catch (Exception e){
                Log.e("Exception",e.getMessage());
            } finally {
                in.close();
            }
        }
    }


    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;

        ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            int current_percent = (int) (100 * mUploaded / mTotal);
            int total_percent = (int) (100 * (mUploaded) / mTotal);
            mFileUploaderCallback.onProgressUpdate(current_percent, total_percent,
                    "File Size: " + ClsGlobal.readableFileSize(filesize));
        }
    }

    public interface FileUploaderCallback {

        void onError();

        void onFinish(String responses);

        void onProgressUpdate(int currentpercent, int totalpercent, String msg);
    }


}
