/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import georegression.struct.shapes.Polygon2D_F64;

/**
 * Describes region inside an image using a polygon and a regionID.
 */
public class PolygonRegion {
    /** Location of the region inside the image. Polygon in pixel coordinates */
    public final Polygon2D_F64 polygon = new Polygon2D_F64();
    /** Region ID for the polygon. */
    public int regionID;

    public void setTo(PolygonRegion src) {
        this.polygon.setTo(src.polygon);
        this.regionID = src.regionID;
    }

    public void reset() {
        polygon.vertexes.resize(0);
        regionID = -1;
    }
}
