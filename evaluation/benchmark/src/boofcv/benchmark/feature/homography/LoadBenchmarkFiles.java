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

package boofcv.benchmark.feature.homography;

import boofcv.struct.feature.TupleDesc_F64;
import bubo.io.serialization.SerializationDefinitionManager;
import bubo.io.text.ReadCsvObjectSmart;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixIO;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class LoadBenchmarkFiles {

	private static SerializationDefinitionManager manager;

	static {
		manager = new SerializationDefinitionManager();
		manager.addPath("boofcv.alg.feature.benchmark.homography");
		manager.addPath("georegression.struct.point");
		manager.loadDefinition(DetectionInfo.class, "location","scale","yaw");
		manager.loadDefinition(FeatureInfo.class, "location","orientation","description");
		manager.loadDefinition(Point2D_F64.class, "x","y");
		manager.loadDefinition(TupleDesc_F64.class,"value" );
	}

	public static List<DetectionInfo> loadDetection( String fileName ) {

		try {
			InputStream stream = new FileInputStream(fileName);
			ReadCsvObjectSmart<DetectionInfo> parser = new ReadCsvObjectSmart<DetectionInfo>(stream,manager,"DetectionInfo");
			List<DetectionInfo> ret = new ArrayList<DetectionInfo>();
			while( true ) {
				DetectionInfo data = parser.nextObject(null);
				if( data != null )
					ret.add(data);
				else
					return ret;
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static List<FeatureInfo> loadDescription( String fileName ) {
		try {
			InputStream stream = new FileInputStream(fileName);
			ReadCsvObjectSmart<FeatureInfo> parser = new ReadCsvObjectSmart<FeatureInfo>(stream,manager,"FeatureInfo");
			int descLength = Integer.parseInt(parser.extractWords().get(0));
			List<FeatureInfo> ret = new ArrayList<FeatureInfo>();
			while( true ) {
				FeatureInfo data = parser.nextObject(new FeatureInfo(descLength));
				if( data != null )
					ret.add(data);
				else
					return ret;
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Homography2D_F32 loadHomography( String fileName ) {
		try {
			DenseMatrix64F M = MatrixIO.loadCSV(fileName,3,3);
			Homography2D_F32 H = new Homography2D_F32();
			H.a11 = (float)M.get(0,0);
			H.a12 = (float)M.get(0,1);
			H.a13 = (float)M.get(0,2);
			H.a21 = (float)M.get(1,0);
			H.a22 = (float)M.get(1,1);
			H.a23 = (float)M.get(1,2);
			H.a31 = (float)M.get(2,0);
			H.a32 = (float)M.get(2,1);
			H.a33 = (float)M.get(2,2);
			return H;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
