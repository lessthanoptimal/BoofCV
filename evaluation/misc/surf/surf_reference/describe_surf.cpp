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

void process( Image *image , FILE *fid , FILE *output)
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

    // Extract the descriptors for the ipts
    int length = des.getVectLength();

    // output the description
    fprintf(output,"%d\n",length);
    for( size_t i = 0; i < ipts.size(); i++ ) {
        Ipoint &p = ipts.at(i);

        des.setIpoint(&p);
        // assign reproducible orientation
        des.assignOrientation();
        // make the SURF descriptor
        des.makeDescriptor();

        fprintf(output,"%f %f %f",(float)p.x,(float)p.y,(float)p.ori);
        for( int i = 0; i < length; i++ ) {
            fprintf(output," %0.10f",p.ivec[i]);
        }
        fprintf(output,"\n");
    }

    printf("Done\n");
}

int main( int argc , char **argv )
{
    if( argc < 3 )
        throw std::runtime_error("[directory] [detected suffix]");

    char *nameDirectory = argv[1];
    char *nameDetected = argv[2];

    char filename[1024];

    printf("directory name: %s\n",nameDirectory);
    printf(" detected name: %s\n",nameDetected);

    for( int i = 1; i <= 6; i++ ) {
        sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,i,nameDetected);
        FILE *fid = fopen(filename,"r");
        if( fid == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }
        sprintf(filename,"%s/img%d.pgm",nameDirectory,i);
        ImLoad ImageLoader;
        Image *img=ImageLoader.readImage(filename);
        if( img == NULL ) {
            fprintf(stderr,"Couldn't open image file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        sprintf(filename,"%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"SURF");
        FILE *output = fopen(filename,"w");
        if( fid == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        printf("Processing %s\n",filename);
        process(img,fid,output);

        fclose(fid);
        fclose(output);
    }

}
