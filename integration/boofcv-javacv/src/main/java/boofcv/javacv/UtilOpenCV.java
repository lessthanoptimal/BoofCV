/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinholeRadial;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.DoubleRawIndexer;
import org.bytedeco.javacpp.opencv_core.*;
import org.ejml.data.DMatrixRMaj;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * Various utility functions for working with OpenCV
 *
 * @author Peter Abeles
 */
public class UtilOpenCV {
	/**
	 * Loads a pinhole camera model with radian and tangential distortion in OpenCV format
	 *
	 * @param fileName path to file
	 * @return CameraPinholeRadial
	 */
	public static CameraPinholeRadial loadPinholeRadial( String fileName ) {
		FileStorage fs = new FileStorage(
				new File(fileName).getAbsolutePath(), FileStorage.READ);

		IntPointer width = new IntPointer(1);
		IntPointer height = new IntPointer(1);

		read(fs.get("image_width"),width,-1);
		read(fs.get("image_height"),height,-1);
		Mat K = new Mat();
		read(fs.get("camera_matrix"),K);
		Mat distortion = new Mat();
		read(fs.get("distortion_coefficients"),distortion);

		CameraPinholeRadial boof = new CameraPinholeRadial();
		boof.width = width.get();
		boof.height = height.get();

		DoubleRawIndexer indexerK = K.createIndexer();
		boof.fx = indexerK.get(0,0);
		boof.skew = indexerK.get(0,1);
		boof.fy = indexerK.get(1,1);
		boof.cx = indexerK.get(0,2);
		boof.cy = indexerK.get(1,2);

		DoubleRawIndexer indexerD = distortion.createIndexer();

		if( distortion.rows() >= 5 )
			boof.setRadial(indexerD.get(0,0),indexerD.get(1,0),indexerD.get(4,0));
		else if( distortion.rows() >= 2 )
			boof.setRadial(indexerD.get(0,0),indexerD.get(1,0));
		if( distortion.rows() >= 5 )
			boof.fsetTangental(indexerD.get(2,0),indexerD.get(3,0));

		return boof;
	}

	public static void save( CameraPinholeRadial model , String fileName ) {
		FileStorage fs = new FileStorage(
				new File(fileName).getAbsolutePath(), FileStorage.WRITE);

		DMatrixRMaj K = PerspectiveOps.calibrationMatrix(model, (DMatrixRMaj)null);

		write(fs,"image_width", model.width);
		write(fs,"image_height", model.height);
		write(fs,"camera_matrix", toMat(K));


		DMatrixRMaj D = new DMatrixRMaj(2+3,1);
		if( model.radial != null ) {
			if( model.radial.length > 0 )
				D.set(0, 0, model.radial[0]);
			if( model.radial.length > 1 )
				D.set(1, 0, model.radial[1]);
			if( model.radial.length > 2 )
				D.set(4, 0, model.radial[2]);
		}
		D.set(2,0,model.t1);
		D.set(3,0,model.t2);

		write(fs,"distortion_coefficients", toMat(D));

		try { fs.close(); } catch (Exception ignore) {}

	}

	public static Mat toMat(DMatrixRMaj in ) {
		Mat out = new Mat(in.numRows,in.numCols,CV_64F);

		DoubleRawIndexer indexer = out.createIndexer();

		for (int i = 0; i < in.numRows; i++) {
			for (int j = 0; j < in.numCols; j++) {
				indexer.put(i,j, in.get(i,j));
			}
		}

		return out;
	}

	// can't find library issues
//	public static List<String> listWebcams() {
//		List<String> output = new ArrayList<>();
//
//		int N = videoInputLib.videoInput.listDevices();
//		for (int i = 0; i < N; i++) {
//			String name = videoInputLib.videoInput.getDeviceName(i).getString();
//			output.add(name);
//		}
//
//		return output;
//	}
//
//	public static OpenCVFrameGrabber findWebcam( String name ) {
//		int N = videoInputLib.videoInput.listDevices();
//		for (int i = 0; i < N; i++) {
//			String device = videoInputLib.videoInput.getDeviceName(i).getString();
//			if( device.equals(name)) {
//				return new OpenCVFrameGrabber(i);
//			}
//		}
//		return null;
//	}
}
