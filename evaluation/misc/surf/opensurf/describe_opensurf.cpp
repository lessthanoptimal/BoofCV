#include "surflib.h"
#include "kmeans.h"
#include <ctime>
#include <iostream>

#include <vector>

using namespace std;

std::vector<string> imageNames;

void process( IplImage *image , FILE *fid , FILE *output)
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

    // Create Surf Descriptor Object
    Surf des(int_img, ipts);

    // Extract the descriptors for the ipts
    des.getDescriptors(false);

    // output the description
    fprintf(output,"64\n");
    for( size_t i = 0; i < ipts.size(); i++ ) {
        Ipoint &p = ipts.at(i);
        fprintf(output,"%7.3f %7.3f %7.5f",p.x,p.y,p.orientation);
        for( int i = 0; i < 64; i++ ) {
            fprintf(output," %0.10f",p.descriptor[i]);
        }
        fprintf(output,"\n");
    }

    printf("Done\n");
    // Deallocate the integral image
    cvReleaseImage(&int_img);
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
        IplImage *img=cvLoadImage(filename);
        if( img == NULL ) {
            fprintf(stderr,"Couldn't open image file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        sprintf(filename,"%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"OpenSURF");
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
