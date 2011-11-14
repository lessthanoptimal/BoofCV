#include "imload.h"
#include "ipoint.h"
#include "image.h"
#include "fasthessian.h"
#include "surf.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace std;
using namespace surf;

std::vector<string> imageNames;

void process( Image *image )
{
    // convert the image into an integral image
    Image iimage(image, false);

    // Create Surf Descriptor Object
    Surf des(&iimage, /* pointer to integral image */
             false, /* double image size flag */
             false, /* rotation invariance or upright */
             false, /* use the extended descriptor */
             4 /* square size of the descriptor window (default 4x4)*/);

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        // detect
        std::vector<Ipoint> ipts;
        FastHessian detector(&iimage,ipts,12.7,false,9,1,4);
        detector.getInterestPoints();

        // describe
        for( size_t i = 0; i < ipts.size(); i++ ) {
            Ipoint &p = ipts.at(i);

            des.setIpoint(&p);
            // assign reproducible orientation
            des.assignOrientation();
            // make the SURF descriptor
            des.makeDescriptor();
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

    sprintf(filename,"%s/img%d.pgm",nameDirectory,imageNumber);
    ImLoad ImageLoader;
    Image *img=ImageLoader.readImage(filename);
    if( img == NULL ) {
        fprintf(stderr,"Couldn't open image file: %s\n",filename);
        throw std::runtime_error("Failed to open");
    }

    printf("Processing %s\n",filename);
    process(img);

    return 0;
}
