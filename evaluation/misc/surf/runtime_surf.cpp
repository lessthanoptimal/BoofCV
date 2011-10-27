
#include "imload.h"
#include "ipoint.h"
#include "image.h"
#include "surf.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace std;
using namespace surf;

std::vector<string> imageNames;

void process( Image *image , FILE *fid )
{
    // convert the image into an integral image
    Image iimage(image, false);

    // location of detected points
    std::vector<Ipoint> ipts;

    // read in location of points
    while( true ) {
        float x,y;
        float scale,yaw;
        int ret = fscanf(fid,"%f %f %f %f\n",&x,&y,&scale,&yaw);
        if( ret != 4 )
            break;
        Ipoint p;
        p.x = x;
        p.y = y;
        p.scale = scale;
        ipts.push_back(p);
    }

    printf("Read in a total of %d points.\n",(int)ipts.size());

    // Create Surf Descriptor Object
    Surf des(&iimage, /* pointer to integral image */
             false, /* double image size flag */
             false, /* rotation invariance or upright */
             false, /* use the extended descriptor */
             4 /* square size of the descriptor window (default 4x4)*/);

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

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
    sprintf(filename,"%s/img%d.pgm",nameDirectory,imageNumber);
    ImLoad ImageLoader;
    Image *img=ImageLoader.readImage(filename);
    if( img == NULL ) {
        fprintf(stderr,"Couldn't open image file: %s\n",filename);
        throw std::runtime_error("Failed to open");
    }

    printf("Processing %s\n",filename);
    process(img,fid);

    fclose(fid);

    return 0;
}
