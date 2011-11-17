#include "cv.h"
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include <highgui.h>
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>

using namespace std;
using namespace cv;

std::vector<string> imageNames;

void process( Mat image )
{

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        // location of detected points
        std::vector<KeyPoint> ipts;

//        SurfFeatureDetector detector(250,4,4,false); // 6485 features
        SurfFeatureDetector detector(3100,4,4,false); // 1999 features
        detector.detect(image,ipts);

        // Create Surf Descriptor Object
        SurfDescriptorExtractor extractor;

        Mat descriptors;
        extractor.compute( image, ipts, descriptors );

        clock_t end = clock();
        long mtime = (long)(1000*(float(end - start) / CLOCKS_PER_SEC));

        if( best == -1 || mtime < best )
            best = mtime;
        printf("time = %d  detected = %d\n",(int)mtime,(int)ipts.size());
    }
    printf("best time = %d\n",(int)best);

}

int main( int argc , char **argv )
{
    if( argc < 2 )
        throw std::runtime_error("[directory]");

    char *nameDirectory = argv[1];

    char filename[1024];

    int imageNumber = 1;

    printf("directory name: %s\n",nameDirectory);
    printf("  image number: %d\n",imageNumber);

    sprintf(filename,"%s/img%d.png",nameDirectory,imageNumber);
    Mat img = imread( filename, CV_LOAD_IMAGE_GRAYSCALE );

    process(img);
}

