#ifndef COMMON_PANOMATIC_HPP
#define COMMON_PANOMATIC_HPP

#include <stdio.h>
#include <vector>
#include "Image.h"
#include "KeyPoint.h"

// Load a pan-o-matic image from a file
libsurf::Image* loadPanImage( char* fileName );

// load keypoints from log file
std::vector<libsurf::KeyPoint *> loadKeyPoint( FILE *fid );

#endif // COMMON_PANOMATIC_HPP
