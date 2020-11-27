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

package boofcv.visualize;

import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * High level interface for displaying point clouds
 *
 * @author Peter Abeles
 */
public interface PointCloudViewer {

	/**
	 * Render the XYZ axis on the screen
	 * @param show
	 */
	void setShowAxis( boolean show );

	void setTranslationStep( double step );

	/**
	 * Dot size when rendered. This is only valid if sprites are being used
	 * @param pixels apparent size of a point
	 */
	void setDotSize( int pixels );

	/**
	 * Specifies the clipping distance. The default value will be infinity or some other very large value
	 * @param distance maximum distance an object away from the camera can be seen
	 */
	void setClipDistance( double distance );

	/**
	 * If true then objects farther away will fade into the background color. Providing some sense of depth.
	 * this is by default off.
	 *
	 * @param active true to turn on
	 */
	void setFog( boolean active );

	void setBackgroundColor( int rgb );

	void addCloud( List<Point3D_F64> cloudXyz , int colorsRgb[] );

	void addCloud( List<Point3D_F64> cloud );

	void addCloud(DogArray_F32 cloudXYZ , DogArray_I32 colorRGB );

	/**
	 * adds a single point to the point cloud. This method can be very slow compared to doing it in a batch
	 */
	void addPoint( double x , double y , double z , int rgb );

	/**
	 * Adds a spite wireframe to the view. Since it's a sprite the thickness is independent of distance.
	 */
	void addWireFrame( List<Point3D_F64> vertexes , boolean closed , int rgb , int radiusPixels );

	/**
	 * Removes all points from the point cloud
	 */
	void clearPoints();

	/**
	 * Used to assign colors to points using a custom function based on their position and/or index. If
	 * a color is specified it will override it
	 */
	void setColorizer( Colorizer colorizer );

	/**
	 * If a colorizer has been specified this will remove it
	 */
	void removeColorizer();

	/**
	 * Specifies the camera's FOV in radians
	 * @param radians FOV size
	 */
	void setCameraHFov(double radians );

	/**
	 * Changes the camera location
	 * @param cameraToWorld transform from camera to world coordinates
	 */
	void setCameraToWorld(Se3_F64 cameraToWorld );

	/**
	 * Returns a copy of the camera to world transform currently being used
	 * @param storage (Optional) storage for the transform
	 * @return The transform
	 */
	Se3_F64 getCameraToWorld( @Nullable Se3_F64 storage );

	/**
	 * Copies the point cloud into the passed in list.
	 *
	 * @param copy Where the cloud should be copied into. if null a new instance is created
	 * @return The copy
	 */
	DogArray<Point3dRgbI_F64> copyCloud(@Nullable DogArray<Point3dRgbI_F64> copy);

	/**
	 * Returns a swing component for adding to a GUI
	 */
	JComponent getComponent();

	/**
	 * Computes the color for a point
	 */
	interface Colorizer {
		int color( int index , double x , double y , double z );
	}
}
