#include "cv.h"
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include <highgui.h>
#include <ctime>
#include <iostream>

#include <vector>

using namespace std;
using namespace cv;

std::vector<string> imageNames;

void process( Mat image , FILE *output)
{
    // detect the features
    std::vector<KeyPoint> ipts;

    SurfFeatureDetector detector(1100,4,4,false);
    detector.detect(image,ipts);

    // save detected points to a file
    for( size_t i = 0; i < ipts.size(); i++ ) {
        KeyPoint &p = ipts.at(i);
        fprintf(output,"%.3f %.3f %.5f %.5f\n",p.pt.x,p.pt.y,p.size,0.0);
    }
    fclose(output);

    printf("Done: %d\n",(int)ipts.size());
}

int main( int argc , char **argv )
{
    if( argc < 2 )
        throw std::runtime_error("[directory]");

    char *nameDirectory = argv[1];

    char filename[1024];

    printf("directory name: %s\n",nameDirectory);

    for( int i = 1; i <= 6; i++ ) {
        sprintf(filename,"%s/img%d.png",nameDirectory,i);
        Mat img = imread( filename, CV_LOAD_IMAGE_GRAYSCALE );

        sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,i,"OpenCV");
        FILE *output = fopen(filename,"w");
        if( output == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        printf("Processing %s\n",filename);
        process(img,output);
    }

}
