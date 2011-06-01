/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.sfm;

import pja.geometry.struct.point.Point2D_F64;


/**
 * Contains the location of a point feature in an image in the key frame and the current frame.
 * Useful for applications where the motion or structure of a scene is computed between
 * two images.
 * 
 * @author Peter Abeles
 */
public class AssociatedPair {
    /**
     * Unique ID associated with this feature
     */
    public long featureId;

    /**
     * Where tracker specific information is stored on this feature.
     */
    public Object description;

    /**
     * Location of the feature in the key frame.
     */
    public Point2D_F64 keyLoc;
    /**
     * Location of the feature in the current.
     */
    public Point2D_F64 currLoc;

    public AssociatedPair(){
        keyLoc = new Point2D_F64();
        currLoc = new Point2D_F64();
    }

    public AssociatedPair(long featureId, double x1, double y1,
                          double x2, double y2)
    {
        this.featureId = featureId;
        keyLoc = new Point2D_F64(x1,y1);
        currLoc = new Point2D_F64(x2,y2);
    }

    /**
     * Creates a new associated point from the two provided points.
     *
     * @param keyLoc first point
     * @param currLoc second point
     * @param newInstance Should it create new points or save a reference to these instances.
     */
    public AssociatedPair(Point2D_F64 keyLoc, Point2D_F64 currLoc, boolean newInstance ) {
        if( newInstance ) {
            this.keyLoc = new Point2D_F64(keyLoc);
            this.currLoc = new Point2D_F64(currLoc);
        } else {
            this.keyLoc = keyLoc;
            this.currLoc = currLoc;
        }
    }

    public <T> T getDescription() {
        return (T) description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }
}
