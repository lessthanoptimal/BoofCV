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

void process( Image *image , FILE *output)
{
    // convert the image into an integral image
    Image iimage(image, false);

    // detect the features
    std::vector<Ipoint> ipts;
    FastHessian detector(&iimage,ipts,6.5,false,9,1,4);
    detector.getInterestPoints();

    // save detected points to a file
    for( size_t i = 0; i < ipts.size(); i++ ) {
        Ipoint &p = ipts.at(i);
        fprintf(output,"%.3f %.3f %.5f %.5f\n",p.x,p.y,p.scale,0.0);
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
        sprintf(filename,"%s/img%d.pgm",nameDirectory,i);
        ImLoad ImageLoader;
        Image *img=ImageLoader.readImage(filename);
        if( img == NULL ) {
            fprintf(stderr,"Couldn't open image file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,i,"SURF");
        FILE *output = fopen(filename,"w");
        if( output == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        printf("Processing %s\n",filename);
        process(img,output);
    }

}
