#include "common_panomatic.hpp"
#include "KeyPointDescriptor.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace std;
using namespace libsurf;

std::vector<string> imageNames;

void process( libsurf::Image *image , FILE *fid )
{
    // location of detected points
    std::vector<libsurf::KeyPoint *> ipts = loadKeyPoint(fid);

    printf("Read in a total of %d points.\n",(int)ipts.size());

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        // Create Surf Descriptor Object
        KeyPointDescriptor desc(*image,false);

        // Describe all the points
        for( size_t i = 0; i < ipts.size(); i++ ) {
            KeyPoint *p = ipts.at(i);

            desc.assignOrientation(*p);
            desc.makeDescriptor(*p);
        }

        clock_t end = clock();
        long mtime = (long)(1000*(float(end - start) / CLOCKS_PER_SEC));

        if( best == -1 || mtime < best )
            best = mtime;
        printf("time = %d\n",(int)mtime);
    }
    printf("best time = %d\n",(int)best);
}

int main( int argc , char **argv )
{
    if( argc < 3 )
        throw std::runtime_error("[directory] [detected suffix]");

    char *nameDirectory = argv[1];
    char *nameDetected = argv[2];

    char filename[1024];

    int imageNumber = 1;

    printf("directory name: %s\n",nameDirectory);
    printf(" detected name: %s\n",nameDetected);
    printf("  image number: %d\n",imageNumber);

    sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,imageNumber,nameDetected);
    FILE *fid = fopen(filename,"r");
    if( fid == NULL ) {
        fprintf(stderr,"Couldn't open file: %s\n",filename);
        throw std::runtime_error("Failed to open");
    }
    sprintf(filename,"%s/img%d.png",nameDirectory,imageNumber);
    libsurf::Image *img = loadPanImage( filename );

    printf("Processing %s\n",filename);
    process(img,fid);

    fclose(fid);

    return 0;
}
