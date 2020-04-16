package com.example.grabcut;


import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;



public class GrabCut {
    static {
        System.loadLibrary("native-lib");
    }

    enum RectState{ NOT_SET, IN_PROCESS, SET };

    private Scalar RED = new Scalar(0,0,255);

    private Scalar BLUE = new Scalar(255,0,0);

    private Scalar GREEN = new Scalar(0,255,0);



    private static int radius = 10;

    private static int thickness = -1;

    public GrabCut(){
        mask = new Mat();
        bgdModel = new Mat();
        fgdModel = new Mat();
        res = new Mat();
        reset();
    }

    private Mat image;

    private Mat mask;

    private Mat bgdModel, fgdModel;
    private List<Point> fgdPxls = new ArrayList<>();
    private List<Point> bgdPxls = new ArrayList<>();

    private RectState rectState, lblsState, prLblsState;

    private boolean isInitialized;

    private Rect rect;
    private Mat res;
    private int iterCount;

    public void reset(){
        reset(mask.getNativeObjAddr());
        if(image != null){
            image.copyTo(res);
        }

        bgdPxls.clear(); fgdPxls.clear();

        isInitialized = false;

        rectState = RectState.NOT_SET;

        lblsState = RectState.NOT_SET;

        prLblsState = RectState.NOT_SET;

        iterCount = 0;
    }

    public void setImage(Mat image) {
        if(image.empty())
            return;
        this.image = image;
        mask.create( image.size(), CvType.CV_8UC1);
        reset();
    }
    public Mat showImage(){
        if(image.empty() )
            return null;

        if(isInitialized ){
            Mat binMask = new Mat();
            Mat r = new Mat();
            getBinMask(mask.getNativeObjAddr(), binMask.getNativeObjAddr() );
            image.copyTo( r, binMask );
            return r;
        }
        if((rectState == RectState.IN_PROCESS || rectState == RectState.SET )){
            Mat r = new Mat();
            res.copyTo(r);
            Imgproc.rectangle( r,new Point( rect.x, rect.y ), new Point(rect.x + rect.width, rect.y + rect.height ), GREEN, 1);
            return r;
        }
        return res;
    }
    public void setRectInMask(){
        assert( !mask.empty() );
        setToBGD(mask.getNativeObjAddr());
        int x = Math.max(0, rect.x);
        int y = Math.max(0, rect.y);
        int width = Math.min(rect.width, image.cols()-x);
        int height = Math.min(rect.height, image.rows()-y);
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        maskRect(mask.getNativeObjAddr(),x,y,width,height);
    }
    public void setLblsInMask( int flags, Point p){
        if( flags == 2 ){
            bgdPxls.add(p);
//            circle(mask.getNativeObjAddr(), (int)p.x,(int)p.y, radius, Imgproc.GC_BGD, thickness );
            Imgproc.circle( res, p, radius, BLUE, thickness );
        }
        if( flags  == 1){
            fgdPxls.add(p);
//            circle(mask.getNativeObjAddr(), (int)p.x,(int)p.y, radius, Imgproc.GC_FGD, thickness );
            Imgproc.circle( res, p, radius, RED, thickness );
        }
    }

    public boolean mouseClick(int event, int x, int y, int flags){
        boolean f = false;
        switch(event){
            case 0: {
                if (flags == 0 && rectState == RectState.NOT_SET) {
                    rectState = RectState.IN_PROCESS;
                    rect = new Rect(x, y, 1, 1);
                }
                if ( flags == 1 && rectState == RectState.SET )
                    lblsState = RectState.IN_PROCESS;
                if ( flags == 2 && rectState == RectState.SET )
                    prLblsState = RectState.IN_PROCESS;
            }
            break;
            case 1:{
                if(flags == 0 || flags == 1){
                    if( rectState == RectState.IN_PROCESS ) {
                        rect = new Rect( new Point(rect.x, rect.y), new Point(x,y) );
                        rectState = RectState.SET;
                        setRectInMask();
                        f = true;
                    }
                    if( lblsState == RectState.IN_PROCESS ) {
                        setLblsInMask(flags,new Point(x,y));
                        lblsState = RectState.SET;
                        f = true;
                    }
                }
                if(flags == 2 && prLblsState == RectState.IN_PROCESS ){
                    setLblsInMask(flags,new Point(x,y));
                    prLblsState = RectState.SET;
                    f = true;
                }
            }
            break;
            case 2:{
                if( rectState == RectState.IN_PROCESS ) {
                    rect =new Rect(new Point(rect.x, rect.y),new Point(x,y) );
                    f = true;
                } else if( lblsState == RectState.IN_PROCESS ) {
                    setLblsInMask(flags,new Point(x,y));
                    f = true;
                } else if( prLblsState == RectState.IN_PROCESS ) {
                    setLblsInMask(flags,new Point(x,y));
                    f = true;
                }
            }
            break;
        }
        return f;
    }
    public int getIterCount(){
        return iterCount;
    }
    public int nextIter(){
        for (Point p:bgdPxls) {
            circle(mask.getNativeObjAddr(), (int)p.x,(int)p.y, radius, Imgproc.GC_BGD, thickness );
        }
        for (Point p:fgdPxls) {
            circle(mask.getNativeObjAddr(), (int)p.x,(int)p.y, radius, Imgproc.GC_FGD, thickness );
        }
        if(isInitialized ){
            Imgproc.grabCut(image, mask, rect, bgdModel, fgdModel, 1 );
        }else {
            if( rectState != RectState.SET )
                return iterCount;
            if( lblsState == RectState.SET || prLblsState == RectState.SET )
                Imgproc.grabCut(image, mask, rect, bgdModel, fgdModel, 1, Imgproc.GC_INIT_WITH_MASK );
            else
                Imgproc.grabCut(image, mask, rect, bgdModel, fgdModel, 1, Imgproc.GC_INIT_WITH_RECT );
            isInitialized = true;
            rect.width = 1;
        }
        iterCount++;
        bgdPxls.clear(); fgdPxls.clear();
        return iterCount;
    }

    private native void reset(long img);
    private native void setToBGD(long img);
    private native void maskRect(long img,int x, int y,int width,int height);
    private native void getBinMask(long comMask, long binMask);
    private native void circle(long img, int x, int y, int radius, int color, int thickness);
}
