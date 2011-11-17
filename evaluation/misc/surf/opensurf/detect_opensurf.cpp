#include "surflib.h"
#include "kmeans.h"
#include <ctime>
#include <iostream>

#include <vector>

using namespace std;

std::vector<string> imageNames;

void process( IplImage *image , FILE *output)
{
    // convert the image into an integral image
    IplImage *int_img = Integral(image);

    // detect the features
    std::vector<Ipoint> ipts;
    FastHessian detector(ipts,4,4,1,0.0008f);

    detector.setIntImage(int_img);
    detector.getIpoints();

    // save detected points to a file
    for( size_t i = 0; i < ipts.size(); i++ ) {
        Ipoint &p = ipts.at(i);
        fprintf(output,"%.3f %.3f %.5f %.5f\n",p.x,p.y,p.scale,0.0);
    }
    fclose(output);

    printf("Done: %d\n",(int)ipts.size());
    // Deallocate the integral image
    cvReleaseImage(&int_img);
}

int main( int argc , char **argv )
{
    if( argc < 2 )
        throw std::runtime_error("[directory]");

    char *nameDirectory = argv[1];

    char filename[1024];

    printf("directory name: %s\n",nameDirectory);

    for( int i = 1; i <= 6; i++ ) {
        sprintf(filename,"%s/img%d.png",nameDirectory,i);
        IplImage *img=cvLoadImage(filename);
        if( img == NULL ) {
            fprintf(stderr,"Couldn't open image file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        sprintf(filename,"%s/DETECTED_img%d_%s.txt",nameDirectory,i,"OpenSURF");
        FILE *output = fopen(filename,"w");
        if( output == NULL ) {
            fprintf(stderr,"Couldn't open file: %s\n",filename);
            throw std::runtime_error("Failed to open");
        }

        printf("Processing %s\n",filename);
        process(img,output);
    }

}
