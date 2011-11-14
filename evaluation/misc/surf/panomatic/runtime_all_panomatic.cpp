
#include "common_panomatic.hpp"
#include "KeyPointDetector.h"
#include "KeyPointDescriptor.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace libsurf;
using namespace std;

std::vector<string> imageNames;

void process( libsurf::Image *image )
{
    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        std::vector<libsurf::KeyPoint> ipts;
        KeyPointDetector detector;
        detector.setMaxOctaves(4);
        detector.setMaxScales(4);
        detector.setScoreThreshold(700000);

        // detect interest points in the image
        KeyPointVectInsertor insertor(ipts);
        detector.detectKeypoints(*image,insertor);

        // Describe interest points
        KeyPointDescriptor desc(*image,false);
        for( size_t i = 0; i < ipts.size(); i++ ) {
            KeyPoint &p = ipts.at(i);

            desc.assignOrientation(p);
            desc.makeDescriptor(p);
        }


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
    libsurf::Image *img = loadPanImage( filename );

    process(img);

    return 0;
}
