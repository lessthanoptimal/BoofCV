#include "common_panomatic.hpp"
#include "KeyPointDescriptor.h"
#include <ctime>
#include <iostream>
#include <stdio.h>

#include <vector>
#include <stdexcept>

using namespace libsurf;
using namespace std;

std::vector<string> imageNames;

void process( libsurf::Image *image , FILE *fid , FILE *output)
{
    // location of detected points
    std::vector<libsurf::KeyPoint *> ipts = loadKeyPoint(fid);

    printf("Read in a total of %d points.\n",(int)ipts.size());

    // Create Surf Descriptor Object
    KeyPointDescriptor desc(*image,false);

    // output the description
    fprintf(output,"64\n");
    for( size_t i = 0; i < ipts.size(); i++ ) {
        KeyPoint *p = ipts.at(i);

        desc.assignOrientation(*p);
        desc.makeDescriptor(*p);

        fprintf(output,"%7.3f %7.3f %7.5f",p->_x,p->_y,p->_ori);
        for( int i = 0; i < 64; i++ ) {
            fprintf(output," %0.10f",p->_vec[i]);
        }
        fprintf(output,"\n");
    }

    printf("Done\n");
    // I'm a bad person for not deallocating everything that was declared with new
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
        sprintf(filename,"%s/img%d.png",nameDirectory,i);
        libsurf::Image *img = loadPanImage( filename );

        sprintf(filename,"%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"PanOMatic");
        FILE *output = fopen(filename,"w");
        if( fid == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        printf("Processing %s\n",filename);
        process(img,fid,output);

        fclose(fid);
    }

}
