

#include "surflib.h"
#include "kmeans.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace std;

std::vector<string> imageNames;

void process( IplImage *image , FILE *fid )
{
    // convert the image into an integral image
    IplImage *int_img = Integral(image);

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

    long best = -1;

    for( int trial = 0; trial < 10; trial++ ) {
        clock_t start = clock();

        // benchmark rules state that integral image calculations should be included in time
        int_img = Integral(image);

        // Create Surf Descriptor Object
        Surf des(int_img, ipts);

        // Extract the descriptors for the ipts
        des.getDescriptors(false);

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
    IplImage *img=cvLoadImage(filename);
    if( img == NULL ) {
        fprintf(stderr,"Couldn't open image file: %s\n",filename);
        throw std::runtime_error("Failed to open");
    }

    printf("Processing %s\n",filename);
    process(img,fid);

    fclose(fid);

    return 0;
}
