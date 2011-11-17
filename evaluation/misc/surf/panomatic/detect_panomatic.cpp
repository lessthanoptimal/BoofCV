
#include "common_panomatic.hpp"
#include "KeyPointDetector.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace libsurf;
using namespace std;

std::vector<string> imageNames;

void process( libsurf::Image *image ,  FILE *output)
{

    std::vector<libsurf::KeyPoint> ipts;
    KeyPointDetector detector;
    detector.setMaxOctaves(4);
    detector.setMaxScales(4);
    detector.setScoreThreshold(380000);

    // detect interest points in the image
    KeyPointVectInsertor insertor(ipts);
    detector.detectKeypoints(*image,insertor);

    // save detected points to a file
    for( size_t i = 0; i < ipts.size(); i++ ) {
        libsurf::KeyPoint &p = ipts.at(i);
        fprintf(output,"%.3f %.3f %.5f %.5f\n",p._x,p._y,p._scale,0.0);
    }
    fclose(output);

    printf("Done:  Detected = %d\n",(int)ipts.size());
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
        libsurf::Image *img = loadPanImage( filename );

        sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,i,"PanOMatic");
        FILE *output = fopen(filename,"w");

        printf("Processing %s\n",filename);
        process(img,output);
    }

}
