/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.core.encoding;


import boofcv.struct.image.*;

/**
 * YUV / YCbCr image format.  The Y component is contained in the width*height block, followed by a (width/2)*(height/2) block
 * for Cb and then a block of the same size for Cr.
 *
 * @author Peter Abeles
 */
public class ConvertYV12 {

    /**
     * Converts a YU12 encoded byte array into a BoofCV formatted image.
     *
     * @param data (input) YU12 byte array
     * @param width (input) image width
     * @param height (input) image height
     * @param output (output) BoofCV image
     */
    public static void yu12ToBoof(byte[] data, int width, int height, ImageBase output) {

        if( output instanceof Planar) {
            Planar ms = (Planar) output;

            if (ms.getBandType() == GrayU8.class) {
                ImplConvertYV12.yv12ToMultiRgb_U8(data, ms);
            } else if (ms.getBandType() == GrayF32.class) {
                ImplConvertYV12.yv12ToMultiRgb_F32(data, ms);
            } else {
                throw new IllegalArgumentException("Unsupported output band format");
            }
        } else if( output instanceof ImageGray) {
            if (output.getClass() == GrayU8.class) {
                yu12ToGray(data, width, height, (GrayU8) output);
            } else if (output.getClass() == GrayF32.class) {
                yu12ToGray(data, width, height, (GrayF32) output);
            } else {
                throw new IllegalArgumentException("Unsupported output type");
            }
        } else if( output instanceof ImageInterleaved ) {
            if( output.getClass() == InterleavedU8.class ) {
                ImplConvertYV12.yv12ToInterleaved(data, (InterleavedU8) output);
            } else if( output.getClass() == InterleavedF32.class ) {
                ImplConvertYV12.yv12ToInterleaved(data, (InterleavedF32) output);
            } else {
                throw new IllegalArgumentException("Unsupported output type");
            }
        } else {
            throw new IllegalArgumentException("Boofcv image type not yet supported");
        }
    }

    /**
     * Converts an YV12 image into a gray scale U8 image.
     *
     * @param data Input: YV12 image data
     * @param width Input: image width
     * @param height Input: image height
     * @param output Output: Optional storage for output image.  Can be null.
     * @return Gray scale image
     */
    public static GrayU8 yu12ToGray(byte[] data , int width , int height , GrayU8 output ) {
        if( output != null ) {
            if( output.width != width || output.height != height )
                throw new IllegalArgumentException("output width and height must be "+width+" "+height);
        } else {
            output = new GrayU8(width,height);
        }

        ImplConvertNV21.nv21ToGray(data, output);

        return output;
    }

    /**
     * Converts an YV12 image into a gray scale F32 image.
     *
     * @param data Input: YV12 image data
     * @param width Input: image width
     * @param height Input: image height
     * @param output Output: Optional storage for output image.  Can be null.
     * @return Gray scale image
     */
    public static GrayF32 yu12ToGray(byte[] data , int width , int height , GrayF32 output ) {
        if( output != null ) {
            if( output.width != width || output.height != height )
                throw new IllegalArgumentException("output width and height must be "+width+" "+height);
        } else {
            output = new GrayF32(width,height);
        }

        ImplConvertNV21.nv21ToGray(data, output);

        return output;
    }
}
