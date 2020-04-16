
#include <jni.h>

#include <string>

#include "opencv2/imgcodecs.hpp"

#include "opencv2/highgui.hpp"

#include "opencv2/imgproc.hpp"





#include <iostream>



using namespace std;

using namespace cv;



const Scalar RED = Scalar(0,0,255);
const Scalar PINK = Scalar(230,130,255);
const Scalar BLUE = Scalar(255,0,0);
const Scalar LIGHTBLUE = Scalar(255,255,160);
const Scalar GREEN = Scalar(0,255,0);





static void getBinMask( const Mat& comMask, Mat& binMask )
{
    if( comMask.empty() || comMask.type()!=CV_8UC1 )
        CV_Error( Error::StsBadArg, "comMask is empty or has incorrect type (not CV_8UC1)" );
    if( binMask.empty() || binMask.rows!=comMask.rows || binMask.cols!=comMask.cols )
        binMask.create( comMask.size(), CV_8UC1 );
    binMask = comMask & 1;
}


class GCApplication

{

public:

    enum{ NOT_SET = 0, IN_PROCESS = 1, SET = 2 };

    static const int radius = 5;

    static const int thickness = -1;



    GCApplication();

    ~GCApplication();

    void reset();

    void setImage(Mat _image);

    void showImage(Mat res) const;


    int mouseClick( int event, int x, int y, int flags);

    int nextIter();

    int getIterCount() const { return iterCount; }

private:

    void setRectInMask();

    void setLblsInMask( int flags, Point p ,bool isPr);

    Mat image;


    Mat mask;

    Mat bgdModel, fgdModel;

    int rectState, lblsState, prLblsState;

    bool isInitialized;



    Rect rect;

    vector<Point> fgdPxls, bgdPxls, prFgdPxls, prBgdPxls;

    int iterCount;

};

GCApplication::GCApplication(){



}

GCApplication::~GCApplication(){



}

void GCApplication::reset(){
    if( !mask.empty() )
        mask.setTo(Scalar::all(GC_BGD));
    bgdPxls.clear(); fgdPxls.clear();
    prBgdPxls.clear();  prFgdPxls.clear();
    isInitialized = false;
    rectState = NOT_SET;
    lblsState = NOT_SET;
    prLblsState = NOT_SET;
    iterCount = 0;
}


void GCApplication::setImage(Mat _image){
    if( _image.empty())
        return;
    image = _image;
    mask.create( image.size(), CV_8UC1);
    reset();
}


void GCApplication::showImage(Mat res) const{
    if(image.empty())
        return ;
    Mat binMask;
    if( !isInitialized )
        image.copyTo(res);
    else{
        getBinMask( mask, binMask );
        image.copyTo(res, binMask );
    }
    vector<Point>::const_iterator it;
    for( it = bgdPxls.begin(); it != bgdPxls.end(); ++it )
        circle( res, *it, radius, BLUE, thickness );
    for( it = fgdPxls.begin(); it != fgdPxls.end(); ++it )
        circle( res, *it, radius, RED, thickness );
    for( it = prBgdPxls.begin(); it != prBgdPxls.end(); ++it )
        circle( res, *it, radius, LIGHTBLUE, thickness );
    for( it = prFgdPxls.begin(); it != prFgdPxls.end(); ++it )
        circle( res, *it, radius, PINK, thickness );
    if( rectState == IN_PROCESS || rectState == SET )
        rectangle( res, Point( rect.x, rect.y ), Point(rect.x + rect.width, rect.y + rect.height ), GREEN, 2);
}



void GCApplication::setRectInMask(){
    CV_Assert( !mask.empty() );
    mask.setTo( GC_BGD );
    rect.x = max(0, rect.x);
    rect.y = max(0, rect.y);
    rect.width = min(rect.width, image.cols-rect.x);
    rect.height = min(rect.height, image.rows-rect.y);
    (mask(rect)).setTo( Scalar(GC_PR_FGD) );
}



void GCApplication::setLblsInMask( int flags, Point p, bool isPr ){
    vector<Point> *bpxls, *fpxls;
    uchar bvalue, fvalue;
    if( !isPr ){
        bpxls = &bgdPxls;
        fpxls = &fgdPxls;
        bvalue = GC_BGD;
        fvalue = GC_FGD;
    }else{
        bpxls = &prBgdPxls;
        fpxls = &prFgdPxls;
        bvalue = GC_PR_BGD;
        fvalue = GC_PR_FGD;
    }
    if( flags == 2 ||  flags == 4)
    {
        bpxls->push_back(p);
        circle( mask, p, radius, bvalue, thickness );
    }
    if(flags == 1 ||  flags == 3)
    {
        fpxls->push_back(p);
        circle( mask, p, radius, fvalue, thickness );
    }
}
// event:DOWN = 0,UP = 1,MOVE = 2
int GCApplication::mouseClick( int event, int x, int y, int flags){

    // TODO add bad args check
    int f = 0;
    switch(event){
        case 0: {
            if (flags == 0 && rectState == NOT_SET) {
                rectState = IN_PROCESS;
                rect = Rect(x, y, 1, 1);
            }
            if ( (flags == 1||flags == 2) && rectState == SET )
                lblsState = IN_PROCESS;
            if ( (flags == 3||flags == 4) && rectState == SET )
                prLblsState = IN_PROCESS;
        }
            break;
        case 1:{

            if(flags == 0 || flags == 1 || flags == 2 ){
                if( rectState == IN_PROCESS ){
                    rect = Rect( Point(rect.x, rect.y), Point(x,y) );
                    rectState = SET;
                    setRectInMask();
                    CV_Assert( bgdPxls.empty() && fgdPxls.empty() && prBgdPxls.empty() && prFgdPxls.empty() );
                    f = 1;
                }
                if( lblsState == IN_PROCESS ){
                    setLblsInMask(flags, Point(x,y),false);
                    lblsState = SET;
                    f = 1;
                }
            }
            if((flags == 3||flags == 4)&& prLblsState == IN_PROCESS ){
                setLblsInMask(flags, Point(x,y), true);
                prLblsState = SET;
                f = 1;
            }
        }
            break;

        case 2:{
            if( rectState == IN_PROCESS ){
                rect = Rect( Point(rect.x, rect.y), Point(x,y) );
                CV_Assert( bgdPxls.empty() && fgdPxls.empty() && prBgdPxls.empty() && prFgdPxls.empty() );
                f = 1;
            }else if( lblsState == IN_PROCESS ){
                setLblsInMask(flags, Point(x,y),false);
                f = 1;
            }else if( prLblsState == IN_PROCESS ){
                setLblsInMask(flags, Point(x,y), true);
                f = 1;
            }
        }
            break;
    }
    return f;
}



int GCApplication::nextIter(){
    if( isInitialized )
        grabCut(image, mask, rect, bgdModel, fgdModel, 1 );
    else
    {
        if( rectState != SET )
            return iterCount;
        if( lblsState == SET || prLblsState == SET )
            grabCut(image, mask, rect, bgdModel, fgdModel, 1, GC_INIT_WITH_MASK );
        else
            grabCut(image, mask, rect, bgdModel, fgdModel, 1, GC_INIT_WITH_RECT );
        isInitialized = true;
    }
    iterCount++;
    bgdPxls.clear(); fgdPxls.clear();
    prBgdPxls.clear(); prFgdPxls.clear();
    return iterCount;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GrabCut_reset(JNIEnv *env, jobject thiz, jlong matAddr) {
    // TODO: implement reset()
    Mat& mat  = *(Mat*)matAddr;
     if(!mat.empty() )
         mat.setTo(Scalar::all(GC_BGD));
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GrabCut_getBinMask(JNIEnv *env, jobject thiz, jlong com_mask,
                                            jlong bin_mask) {
    // TODO: implement getBinMask()
    Mat& comMask = *(Mat*)com_mask;
    Mat& binMask = *(Mat*)bin_mask;
    getBinMask(comMask,binMask);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GrabCut_setToBGD(JNIEnv *env, jobject thiz, jlong matAddr) {
    // TODO: implement maskSetTo()
    Mat& mat  = *(Mat*)matAddr;
    mat.setTo(GC_BGD);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GrabCut_circle(JNIEnv *env, jobject thiz, jlong img, jint center_x,
                                        jint center_y, jint radius, jint color, jint thickness) {
    // TODO: implement circle()
    Mat& mat  = *(Mat*)img;
    circle( mat, Point(center_x,center_y), radius, color, thickness );
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GrabCut_maskRect(JNIEnv *env, jobject thiz, jlong img,jint x, jint y,jint width,jint height) {
    // TODO: implement maskRect()
    Mat& mat  = *(Mat*)img;
    Rect rect = Rect(x, y, width, height);
    mat(rect).setTo(Scalar(GC_PR_FGD));
}extern "C"
JNIEXPORT GCApplication * JNICALL
Java_com_example_grabcut_GCActivity_initGrabCut(JNIEnv *env, jobject thiz, jlong image) {
    // TODO: implement initGrabCut()
    Mat& img = *(Mat *) image ;
    GCApplication *gcapp = new GCApplication();
    gcapp->setImage(img);
    return gcapp;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_grabcut_GCActivity_touchMove(JNIEnv *env, jobject thiz, jint event, jint x,
                                                jint y, jint flags, jlong gcapp) {
    // TODO: implement moveGrabCut()
    GCApplication *g = (GCApplication *) gcapp;
    return g->mouseClick( event, x, y, flags);
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GCActivity_reset(JNIEnv *env, jobject thiz, jlong gcapp) {
    // TODO: implement reset()
    GCApplication *g = (GCApplication *) gcapp;
    g->reset();
}extern "C"
JNIEXPORT void JNICALL
Java_com_example_grabcut_GCActivity_showImage(JNIEnv *env, jobject thiz, jlong image, jlong gcapp) {
    // TODO: implement showImage()
    Mat& img = *(Mat *) image ;
    GCApplication *g = (GCApplication *) gcapp;
    g->showImage(img);
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_grabcut_GCActivity_grabCut(JNIEnv *env, jobject thiz, jlong gcapp) {
    // TODO: implement grabCut()

    GCApplication *g = (GCApplication *) gcapp;
    int iterCount = g->getIterCount();
    int newIterCount = g->nextIter();
    return (jboolean) (newIterCount > iterCount);
}