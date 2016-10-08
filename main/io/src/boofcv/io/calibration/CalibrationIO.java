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

package boofcv.io.calibration;

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import com.google.protobuf.TextFormat;

import java.io.*;
import java.net.URL;

/**
 * @author Peter Abeles
 */
public class CalibrationIO {

	/**
	 * Saves a camera model/parameters to disk.  Saved as a protobuf
	 *
	 * @param parameters Camera parameters
	 * @param filePath Path to where it should be saved
	 */
	public static <T extends CameraPinhole> void save(T parameters , String filePath ) {
		PrintStream out;
		try {
			out = new PrintStream(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		ProtoCameraModels.Pinhole.Builder p = ProtoCameraModels.Pinhole.newBuilder();

		p.setWidth( parameters.getWidth() );
		p.setHeight( parameters.getHeight() );
		p.setFx( parameters.fx );
		p.setFy( parameters.fy );
		p.setCx( parameters.cx );
		p.setCy( parameters.cy );
		p.setSkew( parameters.skew );

		try {
		if( parameters instanceof CameraPinholeRadial) {
			CameraPinholeRadial radial = (CameraPinholeRadial)parameters;

			ProtoCameraModels.PinholeRadial.Builder pr = ProtoCameraModels.PinholeRadial.newBuilder();

			pr.setPinhole(p.build());
			pr.setT1(radial.getT1());
			pr.setT2(radial.getT2());
			if( radial.radial != null ) {
				for (int i = 0; i < radial.radial.length; i++) {
					pr.addRadial(radial.radial[i]);
				}
			}

			out.println("# Pinhole camera model with radial and tangential distortion");
			out.println("# textual protobuf format");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println("# radial = radial distortion, t1,t2) = tangential distortion");
			out.println();
			TextFormat.print(pr,out);
		} else {
			out.println("# Pinhole camera model");
			out.println("# textual protobuf format");
			out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
			out.println();
			TextFormat.print(p,out);
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		out.close();
	}

	public static <T extends CameraPinhole> void save( T parameters , File filePath ) {
		save(parameters, filePath.getAbsolutePath());
	}

	/**
	 * Loads a pinhole camera model from disk
	 * @param reader Input to text
	 * @return The camera model
	 */
	public static CameraPinhole loadPinhole( Reader reader ) {
		try {
//			ProtoCameraModels.Pinhole p =
//					ProtoCameraModels.Pinhole.parseFrom(new FileInputStream(filePath));

			ProtoCameraModels.Pinhole.Builder builder = ProtoCameraModels.Pinhole.newBuilder();
			TextFormat.merge(reader, builder);

			ProtoCameraModels.Pinhole p = builder.build();

			CameraPinhole model = new CameraPinhole();
			model.width = p.getWidth();
			model.height = p.getHeight();
			model.skew = p.getSkew();
			model.cx = p.getCx();
			model.cy = p.getCy();
			model.fx = p.getFx();
			model.fy = p.getFy();

			return model;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Loads a pinhole camera model with radial and tangential distortion from disk
	 * @param reader Input to text
	 * @return The camera model
	 */
	public static CameraPinholeRadial loadPinholeRadial( Reader reader ) {
		try {
//			ProtoCameraModels.PinholeRadial pr =
//					ProtoCameraModels.PinholeRadial.parseFrom(new FileInputStream(filePath));
			ProtoCameraModels.PinholeRadial.Builder builder = ProtoCameraModels.PinholeRadial.newBuilder();
			TextFormat.merge(reader, builder);

			ProtoCameraModels.PinholeRadial pr = builder.build();

			ProtoCameraModels.Pinhole p = pr.getPinhole();

			CameraPinholeRadial model = new CameraPinholeRadial();
			model.width = p.getWidth();
			model.height = p.getHeight();
			model.skew = p.getSkew();
			model.cx = p.getCx();
			model.cy = p.getCy();
			model.fx = p.getFx();
			model.fy = p.getFy();

			if( pr.hasT1() )
				model.t1 = pr.getT1();
			if( pr.hasT2() )
				model.t2 = pr.getT2();

			if( pr.getRadialCount() > 0 ) {
				model.radial = new double[pr.getRadialCount()];
				for (int i = 0; i < model.radial.length; i++) {
					model.radial[i] = pr.getRadial(i);
				}
			}

			return model;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T load( String directory , String name ) {
		return load( new File(directory,name));
	}

	public static <T> T load( URL filePath ) {
		return load( filePath.getPath());
	}

	public static <T> T load( File filePath ) {
		return load( filePath.getAbsolutePath());
	}

	public static <T> T load( String filePath ) {
		try {
			return load( new FileReader(filePath));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public static <T> T load( Reader reader ) {
		// read the string into an array so that it can be read multiple times
		String text = "";
		char tmp[] = new char[1024];
		while( true ) {
			try {
				int length = reader.read(tmp,0,tmp.length);
				if( length > 0 ) {
					text += new String(tmp,0,length);
				}
				if( length != tmp.length )
					break;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			return (T) loadPinholeRadial(new StringReader(text));
		} catch (RuntimeException e) {
		}
		try {
			return (T) loadPinhole(new StringReader(text));
		} catch (RuntimeException e) {
		}

		throw new RuntimeException("Unknown model type");
	}
}
