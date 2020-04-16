package com.example.grabcut;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;

public class GCActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }
    private int flags = 0; // 0范围，1前景，2背景
    private Bitmap bitmap;
    private ImageView imageView;
    private Bitmap bm;
    private float s = 0;

    private long gcapp;
    private Mat img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gc);
        imageView = findViewById(R.id.image_view);


        bitmap = decodeResource(getResources(),R.drawable.timg);
        bm = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);


        img = new Mat();
        Utils.bitmapToMat(bitmap, img);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2RGB);
        imageView.setImageBitmap(bitmap);
        gcapp = initGrabCut(img.getNativeObjAddr());

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(s == 0){
                    s = imageView.getWidth()*1.0f / bitmap.getWidth();
                }

                final int x = (int) (event.getX()/s);

                final int y = (int) (event.getY()/s);

                int type = event.getAction();

                switch (type){
                    case MotionEvent.ACTION_DOWN:
                        if(touchMove(0,x,y,flags,gcapp) == 1){
                            Mat res = new Mat();
                            res.create(img.size(),img.type());
                            showImage(res.getNativeObjAddr(),gcapp);
                            Utils.matToBitmap(res,bm);
                            imageView.setImageBitmap(bm);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(touchMove(1,x,y,flags,gcapp) == 1){
                            Mat res = new Mat();
                            res.create(img.size(),img.type());
                            showImage(res.getNativeObjAddr(),gcapp);
                            Utils.matToBitmap(res,bm);
                            imageView.setImageBitmap(bm);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(touchMove(2,x,y,flags,gcapp) == 1){
                            Mat res = new Mat();
                            res.create(img.size(),img.type());
                            showImage(res.getNativeObjAddr(),gcapp);
                            Utils.matToBitmap(res,bm);
                            imageView.setImageBitmap(bm);
                        }
                        break;
                }
                return true;

            }

        });
    }
    //BitmapFactory.decodeResource()得到的Bitmap长宽变大,修改一下得到原宽高
    public Bitmap decodeResource(Resources res, int id) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = null;
        InputStream is = null;
        try {
            final TypedValue value = new TypedValue();
            is = res.openRawResource(id, value);
            opts.inTargetDensity = value.density;
            opts.inScaled = false;
            bm = BitmapFactory.decodeResourceStream(res, value, is, null, opts);
        } catch (Exception e) {
            /*  do nothing.
                If the exception happened on open, bm will be null.
                If it happened on close, bm is still valid.
            */
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        if (bm == null && opts.inBitmap != null) {
            throw new IllegalArgumentException("Problem decoding into existing bitmap");
        }
        return bm;
    }


    public void onFlags(View view){
        Button button = (Button) view;
        if("范围".equals(button.getText().toString())){
            flags = 0;
        }else if("前景".equals(button.getText().toString())){
            flags = 1;
        }else if("背景".equals(button.getText().toString())){
            flags = 2;
        }else if("可能前景".equals(button.getText().toString())){
            flags = 3;
        }else if("可能背景".equals(button.getText().toString())){
            flags = 4;
        }
    }

    public void onReset(View view){
        flags = 0;
        reset(gcapp);
        imageView.setImageBitmap(bitmap);
    }

    public void onGrabCut(View view){
        new Thread(){
            @Override
            public void run() {
                super.run();
                Log.d("grabCut","开始处理");
                if(grabCut(gcapp)){
                    Mat res = new Mat();
                    res.create(img.size(),img.type());
                    showImage(res.getNativeObjAddr(),gcapp);
                    Utils.matToBitmap(res,bm);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("grabCut","显示");
                            imageView.setImageBitmap(bm);
                        }
                    });

                }
                Log.d("grabCut","结束处理");
            }

        }.start();
    }


    public native long initGrabCut(long image);

    public native int touchMove(int event, int x, int y, int flags,long gcapp);
    public native void reset(long gcapp);
    public native void showImage(long image,long gcapp);
    public native boolean grabCut(long gcapp);
}
