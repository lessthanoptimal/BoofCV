/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.array.PolynomialNevilleFixed_F32;
import boofcv.struct.image.ImageFloat32;


/**
 * <p>
 * A quick and dirty arbitrary degree polynomial interpolation algorithm for images.
 * A M by M region is the image is used in the interpolation.  Edges are handled
 * with no problems.  In the region first the interpolation is done horizontally,
 * then it is done vertically along the interpolated column that was just computed.
 * </p>
 * <p>
 * The code is unopitimized and the algorithm is relatively expensive.
 * </p>
 * @author Peter Abeles
 */
public class ImplPolynomialPixel_F32 implements InterpolatePixel<ImageFloat32>  {
    // the image that is being interpolated
    private ImageFloat32 image;

    private int M;
    // if even need to add one to initial coordinate to make sure
    // the point interpolated is bounded inside the interpolation points
    private int offM;

    // temporary arrays used in the interpolation
    private float horiz[];
    private float vert[];

    // the minimum and maximum pixel intensity values allowed
    private float min;
    private float max;

    private PolynomialNevilleFixed_F32 interp1D;

    public ImplPolynomialPixel_F32(int maxDegree, float min, float max) {
        this.M = maxDegree;
        this.min = min;
        this.max = max;
        horiz = new float[maxDegree];
        vert = new float[maxDegree];

        if( maxDegree % 2 == 0 ) {
            offM = 1;
        } else {
            offM = 0;
        }

        interp1D = new PolynomialNevilleFixed_F32(maxDegree);
    }

    @Override
    public void setImage(ImageFloat32 image) {
        this.image = image;
    }

    @Override
    public ImageFloat32 getImage() {
        return image;
    }

    // todo reduce the order of the interpolation at edges to improve results
    @Override
    public float get(float x, float y) {
        int width = image.getWidth();
        int height = image.getHeight();

        final int xt = (int) x;
        final int yt = (int) y;

        // offM makes sure even numbered M will bound the test point with samples
        int x0 = xt - M/2 + offM;
        int x1 = x0 + M;
        int y0 = yt - M/2 + offM;
        int y1 = y0 + M;

        if( x0 < 0 ) { x0 = 0;}
        else if( x1 > width) {x1 = width;}

        if( y0 < 0 ) { y0 = 0;}
        else if( y1 > height) {y1 = height;}

        final int horizM = x1-x0;
        final int vertM = y1-y0;

        interp1D.setInput(horiz,horiz.length);
        for( int i = 0; i < vertM; i++ ) {
            for( int j = 0; j < horizM; j++ ) {
                horiz[j] = image.get(j+x0,i+y0);
            }
            vert[i]=interp1D.process(x-x0,0,horizM-1);
        }
        interp1D.setInput(vert,vert.length);

        float ret = interp1D.process(y-y0,0,vertM-1);

        // because it is fitting polynomials it can go above and below max values.
        if( ret > max ) {
            ret = max;
        } else if( ret < min ) {
            ret = min;
        }
        return ret;
    }

    @Override
    public float get_unsafe(float x, float y) {
       int xt = (int) x;
        int yt = (int) y;

        int x0 = xt - M/2 + offM;
        int y0 = yt - M/2 + offM;

        interp1D.setInput(horiz,horiz.length);
        for( int i = 0; i < M; i++ ) {
            for( int j = 0; j < M; j++ ) {
                horiz[j] = image.get(j+x0,i+y0);
            }
            vert[i]=interp1D.process(x-x0,0,M-1);
        }
        interp1D.setInput(vert,vert.length);

        float ret = interp1D.process(y-y0,0,M-1);

        // because it is fitting polynomials it can go above or below max or min values.
        if( ret > max ) {
            ret = max;
        } else if( ret < min ) {
            ret = min;
        }
        return ret;
    }
}
