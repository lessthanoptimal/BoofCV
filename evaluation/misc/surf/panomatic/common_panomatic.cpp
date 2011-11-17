#include "common_panomatic.hpp"

#include "cv.h"
#include "opencv2/core/core.hpp"
#include "highgui.h"

using namespace libsurf;
using namespace std;
using namespace cv;

libsurf::Image* loadPanImage( char* fileName ) {
    Mat cvimg = imread( fileName, CV_LOAD_IMAGE_GRAYSCALE );

    int width = cvimg.cols;
    int height = cvimg.rows;

    printf("Image width=%d height=%d type = %d depth = %d channels = %d\n",width,height,cvimg.type(),cvimg.depth(),cvimg.channels());

    double **data = new double*[height];
    for( int y = 0; y < height; y++ )
        data[y] = new double[width];

    for( int y = 0; y < height; y++ ) {
        for( int x = 0; x < width; x++ ) {
            uint8_t pixel = cvimg.at<uint8_t>(y,x);
            data[y][x] = pixel;
        }
    }

    return new Image(data,width,height);
}

std::vector<libsurf::KeyPoint *> loadKeyPoint( FILE *fid ) {

    std::vector<libsurf::KeyPoint *> list;

    // read in location of points
    while( true ) {
        float x,y;
        float scale,yaw;
        int ret = fscanf(fid,"%f %f %f %f\n",&x,&y,&scale,&yaw);
        if( ret != 4 )
            break;

        libsurf::KeyPoint *p = new libsurf::KeyPoint(x,y,scale,1000,200);
        list.push_back(p);
    }

    return list;
}
