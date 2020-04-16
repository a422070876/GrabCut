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
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;

public class RectActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }
    private Scalar GREEN = new Scalar(0,255,0);
    private Bitmap bitmap;
    private ImageView imageView;
    private Bitmap bm;
    private float s = 0;

    private Mat image;
    private int downX,downY;

    private Rect rect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rect);
        imageView = findViewById(R.id.image_view);

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
                        downX = x;
                        downY = y;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_MOVE:
                        rect = new Rect( new Point(downX, downY), new Point(x,y) );
                        Mat res = new Mat();
                        image.copyTo(res);
                        Imgproc.rectangle(res,new Point( downX, downY ), new Point(x , y  ), GREEN, 1);
                        Utils.matToBitmap(res,bm);
                        imageView.setImageBitmap(bm);
                        break;
                }
                return true;

            }

        });
        bitmap = decodeResource(getResources(),R.drawable.timg);
        bm = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        image = new Mat();

        Utils.bitmapToMat(bitmap, image);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        imageView.setImageBitmap(bitmap);

    }
    public void onGrabCut(View view){
        new Thread(){
            @Override
            public void run() {
                super.run();

                Mat firstMask = new Mat();
                Mat bgModel = new Mat();
                Mat fgModel = new Mat();
                Imgproc.grabCut(image, firstMask, rect, bgModel, fgModel, 1, Imgproc.GC_INIT_WITH_RECT );

                Mat source = new Mat(firstMask.rows(), firstMask.cols(), CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
                Core.compare(firstMask, source, firstMask, Core.CMP_EQ);

                Mat res = new Mat();
                image.copyTo(res,firstMask);

                Utils.matToBitmap(res,bm);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bm);
                    }
                });


            }
        }.start();
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

}
