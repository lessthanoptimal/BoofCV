#ifndef COMMON_PANOMATIC_HPP
#define COMMON_PANOMATIC_HPP

#include <stdio.h>
#include <vector>
#include "Image.h"
#include "KeyPoint.h"
#include "KeyPointDetector.h"

// Load a pan-o-matic image from a file
libsurf::Image* loadPanImage( char* fileName );

// load keypoints from log file
std::vector<libsurf::KeyPoint *> loadKeyPoint( FILE *fid );

// define a Keypoint insertor
class KeyPointVectInsertor : public libsurf::KeyPointInsertor
{
public:
        KeyPointVectInsertor(std::vector<libsurf::KeyPoint>& _v) : v(_v) {};
        inline virtual void operator()(const libsurf::KeyPoint &k)
        {
                v.push_back(k);
        }

private:
        std::vector<libsurf::KeyPoint>& v;

};

#endif // COMMON_PANOMATIC_HPP
