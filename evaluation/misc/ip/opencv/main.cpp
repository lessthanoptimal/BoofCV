#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace cv;
using namespace std;

const int minTestTime = 1000;
char imageLocation[] = "../../../../data/evaluation/standard/barbara.png";
char imageLineLocation[] = "../../../../data/evaluation/simple_objects.jpg";
Mat inputImage;
Mat lineImage;


class Performer
{
public:
    virtual void process() = 0;

};

long measureTime( Performer *performer , int num )
{
        clock_t startTime = clock();
        for( int i = 0; i < num; i++ ) {
                performer->process();
        }
        clock_t stopTime = clock();

        return (long)(1000*(float(stopTime - startTime) / CLOCKS_PER_SEC));
}

double profile( Performer *performer ) {

    int N = 1;
    long elapsedTime;
    while( true ) {
        elapsedTime = measureTime(performer,N);
        if(elapsedTime >= minTestTime)
            break;
        N = N*2;
    }

    return (double)N/(elapsedTime/1000.0);
}

class PerformerGaussian : public Performer
{
public:
    Mat dst;

    PerformerGaussian() {
        dst = inputImage.clone();
    }

    virtual void process() {
        GaussianBlur( inputImage, dst, Size( 5, 5 ), 0 , 0 );
    }
};

class PerformerSobel : public Performer
{
public:
    Mat derivX,derivY;

    PerformerSobel() {
        // do it once to initialize image data.  not sure if this is really necissary
        Sobel( inputImage, derivX, CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT );
        Sobel( inputImage, derivY, CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT );
    }

    virtual void process() {
        Sobel( inputImage, derivX, CV_16S, 1, 0, 3, 1, 0, BORDER_DEFAULT );
        Sobel( inputImage, derivY, CV_16S, 0, 1, 3, 1, 0, BORDER_DEFAULT );
    }
};

class PerformerHarris : public Performer
{
public:

    virtual void process() {
        vector<Point2f> corners;

        // no direct correspondence between BoofCV threshold and OpenCV.
        // adjusted threshold until it found about 2450 features
        goodFeaturesToTrack( inputImage, corners,
                       100000, 0.00035,  2, Mat(), 5, true, 0.04 );
        //printf("num found %d\n",(int)corners.size());

    }
};

class PerformerCanny : public Performer
{
public:

    Mat canny_output;

    virtual void process() {
        double low=5;
        double high=50;

        Canny( inputImage, canny_output, low,high, 3 );
    }
};

class PerformerContour : public Performer
{
public:

    Mat binary;

    virtual void process() {
        threshold( inputImage, binary, 75, 255,0 );
        vector<vector<Point> > contours;
        findContours( binary, contours, CV_RETR_LIST, CV_CHAIN_APPROX_NONE, Point(0, 0) );
    }
};

class PerformerHough : public Performer
{
public:

    virtual void process() {
        vector<Point2f> lines;

        // adjusted count threshold to produce same number of lines as BoofCV, about 25
        // BoofCV is more confligurable and don't know exactly what this alg is doing
        HoughLines( lineImage, lines,2,3.14/180.0,2000);
//        printf("num found %d\n",(int)lines.size());

    }
};

class PerformerSURF : public Performer
{
public:
    SurfFeatureDetector detector;
    SurfDescriptorExtractor extractor;

    // tuned threshold until it got close to 1112, number of features BoofCV found
    PerformerSURF() : detector(325,4,4,false)
    {

    }

    virtual void process() {
        std::vector<KeyPoint> ipts;
        detector.detect(inputImage,ipts);

        Mat descriptors;
        extractor.compute( inputImage, ipts, descriptors );

//        printf("num points %d\n",(int)ipts.size());
    }
};

int main( int argc, char** argv )
{
    inputImage = imread( imageLocation, IMREAD_GRAYSCALE );
    lineImage = imread( imageLineLocation, IMREAD_GRAYSCALE );

    printf("=========  Profile Description width = %d height = %d\n",inputImage.cols,inputImage.rows);


    PerformerGaussian gauss;
    PerformerSobel sobel;
    PerformerHarris harris;
    PerformerCanny canny;
    PerformerContour contour;
    PerformerHough hough;
    PerformerSURF surf;

    printf("Gaussian    = %6.3f\n",profile(&gauss));
    printf("Sobel       = %6.3f\n",profile(&sobel));
    printf("Harris      = %6.3f\n",profile(&harris));
    printf("Canny       = %6.3f\n",profile(&canny));
    printf("Contour     = %6.3f\n",profile(&contour));
    printf("Hough Lines = %6.3f\n",profile(&hough));
    printf("SURF        = %6.3f\n",profile(&surf));

    return 0;
}
