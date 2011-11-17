
#include "surflib.h"
#include "kmeans.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace std;

std::vector<string> imageNames;

void process( IplImage *image )
{

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        std::vector<Ipoint> ipts;
        FastHessian detector(ipts,4,4,1,0.0013f);

        // convert the image into an integral image
        IplImage *int_img = Integral(image);

        // detect features in the image
        detector.setIntImage(int_img);
        detector.getIpoints();

        // Create Surf Descriptor Object
        Surf des(int_img, ipts);

        // Extract the descriptors for the ipts
        des.getDescriptors(false);

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
    IplImage *img=cvLoadImage(filename);
    if( img == NULL ) {
        fprintf(stderr,"Couldn't open image file: %s\n",filename);
        throw std::runtime_error("Failed to open");
    }

    process(img);

    return 0;
}
