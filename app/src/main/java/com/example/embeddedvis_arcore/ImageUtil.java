package com.example.embeddedvis_arcore;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class ImageUtil {

    public byte[] imageToByteArray(Image image, int rotation) {
        byte[] data = null;

        // 에러떠서 하드코딩 640 480 image.getWidth(), image.getHeight()
        int width = image.getWidth();
        int height = image.getHeight();

        data = NV21toJPEG( YUV_420_888toNV21(image), width, height);

        String wid = String.valueOf(width);
        String hei = String.valueOf(height);

        Log.d(TAG, "imageToByteArray: " + wid + ", " + hei );
//        doInBackground(data);

        return data;
    }

    public byte[] YUV_420_888toNV21(Image image) {

        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();


        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 70, out);
        return out.toByteArray();
    }

    private String getBase64encode(byte[] data){
        return Base64.encodeToString(data, 0);

    }

    public String YUV2Base64(Image image, int rotation) {
        byte[] data = imageToByteArray(image, rotation);
        return getBase64encode(data);
    }


    public void doInBackground(byte[]... jpeg) {
        File photo=new File(Environment.getExternalStorageDirectory(), "photo.jpg");

        if (photo.exists()) {
            photo.delete();
        }

        try {
            FileOutputStream fos=new FileOutputStream(photo.getPath());

            fos.write(jpeg[0]);
            fos.close();
            Log.d("PictureSuccess", "doInBackground: success!");
        }
        catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }

}
