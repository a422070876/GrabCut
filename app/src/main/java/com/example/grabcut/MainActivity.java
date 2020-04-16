package com.example.grabcut;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;


public class MainActivity extends AppCompatActivity {



    // Used to load the 'native-lib' library on application startup.

    static {
        System.loadLibrary("native-lib");
    }

//    private long gcapp;
    private int flags = 0; // 0范围，1前景，2背景
    private Bitmap bitmap;
    private ImageView imageView;
    private Bitmap bm;
    private float s = 0;

    private GrabCut grabCut;

    private Handler grabCutHandler;
    private HandlerThread grabCutThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grabCutThread = new HandlerThread("grabCut");
        grabCutThread.start();
        grabCutHandler = new Handler(grabCutThread.getLooper());



        imageView = findViewById(R.id.image_view);

        Bitmap b = BitmapFactory.decodeResource(getResources(),R.drawable.timg);
        bitmap = Bitmap.createScaledBitmap(b,b.getWidth()/3,b.getHeight()/3,false);
        bm = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);


        Mat img = new Mat();
        Utils.bitmapToMat(bitmap, img);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2RGB);

//        gcapp = initGrabCut(img.getNativeObjAddr());

        grabCut = new GrabCut();
        grabCut.setImage(img);
        showImage(grabCut.showImage());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDraw = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isDraw){
                    return true;
                }
                if(s == 0){
                    s = imageView.getWidth()*1.0f / bitmap.getWidth();
                }

                final int x = (int) (event.getX()/s);

                final int y = (int) (event.getY()/s);

                int type = event.getAction();

                switch (type){
                    case MotionEvent.ACTION_DOWN:
                        isDraw = true;
                        grabCutHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(grabCut.mouseClick(0,x,y,flags)){
                                    Mat m = grabCut.showImage();
                                    Utils.matToBitmap(m,bm);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            isDraw = false;
                                            imageView.setImageBitmap(bm);
                                        }
                                    });
                                }else{
                                    isDraw = false;
                                }

                            }
                        });

//                        moveGrabCut(0,x,y,flags,gcapp);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDraw = true;
                        grabCutHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(grabCut.mouseClick(1,x,y,flags)){
                                    Mat m = grabCut.showImage();
                                    Utils.matToBitmap(m,bm);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            isDraw = false;
                                            imageView.setImageBitmap(bm);
                                        }
                                    });
                                }else{
                                    isDraw = false;
                                }

                            }
                        });
//                        moveGrabCut(1,x,y,flags,gcapp);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isDraw = true;
                        grabCutHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(grabCut.mouseClick(2,x,y,flags)){
                                    Mat m = grabCut.showImage();
                                    Utils.matToBitmap(m,bm);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            isDraw = false;
                                            imageView.setImageBitmap(bm);
                                        }
                                    });
                                }else{
                                    isDraw = false;
                                }
                            }
                        });
//                        moveGrabCut(2,x,y,flags,gcapp);
                        break;

                }
                return true;

            }

        });
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
        grabCut.reset();
        showImage(grabCut.showImage());
//        reset(gcapp);

    }

    public void onGrabCut(View view){

        Thread thread = new Thread(){

            @Override

            public void run() {
                super.run();
                Log.d("grabCut","开始处理");
                int iterCount = grabCut.getIterCount();
                if(grabCut.nextIter() > iterCount){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("grabCut","显示");
                            showImage(grabCut.showImage());
                        }
                    });
                }
                Log.d("grabCut","结束处理");
//                if(grabCut(gcapp)){
//
//                    runOnUiThread(new Runnable() {
//
//                        @Override
//
//                        public void run() {
//                            grabCutOver(gcapp);
//                        }
//
//                    });
//
//                }





            }

        };

        thread.start();

    }



    public void showImage(Mat img){
        Utils.matToBitmap(img,bm);
        imageView.setImageBitmap(bm);
    }



//    public native long initGrabCut(long image);
//
//    public native void moveGrabCut(int event, int x, int y, int flags,long gcapp);
//
//    public native void reset(long gcapp);
//
//    public native boolean grabCut(long gcapp);
//
//    public native void grabCutOver(long gcapp);

}
