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

package boofcv.examples;

import boofcv.alg.depth.VisualDepthOps;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Loads kinect observations and saves a point cloud with rgb information to a file in a simple CSV format
 *
 * @author Peter Abeles
 */
public class CreateRgbPointCloudFileApp {
	public static void main( String args[] ) throws IOException {
		String baseDir = "log/";

		String nameRgb = baseDir+"rgb0000000.ppm";
		String nameDepth = baseDir+"depth0000000.depth";
		String nameCalib = baseDir+"intrinsic.yaml";

		CameraPinholeRadial param = CalibrationIO.load(nameCalib);

		GrayU16 depth = new GrayU16(1,1);
		Planar<GrayU8> rgb = new Planar<>(GrayU8.class,1,1,3);

		UtilImageIO.loadPPM_U8(nameRgb, rgb, null);
		UtilOpenKinect.parseDepth(nameDepth,depth,null);

		FastQueue<Point3D_F64> cloud = new FastQueue<Point3D_F64>(Point3D_F64.class,true);
		FastQueueArray_I32 cloudColor = new FastQueueArray_I32(3);

		VisualDepthOps.depthTo3D(param, rgb, depth, cloud, cloudColor);

		DataOutputStream file = new DataOutputStream(new FileOutputStream("kinect_pointcloud.txt"));

		file.write("# Kinect RGB Point cloud. Units: millimeters. Format: X Y Z R G B\n".getBytes());

		for( int i = 0; i < cloud.size; i++ ) {
			Point3D_F64 p = cloud.get(i);
			int[] color = cloudColor.get(i);

			String line = String.format("%.10f %.10f %.10f %d %d %d\n",p.x,p.y,p.z,color[0],color[1],color[2]);
			file.write(line.getBytes());
		}
		file.close();

		System.out.println("Total points = "+cloud.size);
	}
}
