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
import boofcv.struct.calib.StereoParameters;
import com.google.protobuf.TextFormat;
import georegression.struct.se.Se3_F64;

import java.io.*;
import java.net.URL;

/**
 * Functions for loading and saving camera calibration related data structures from/to disk
 *
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

		try {
			if( parameters instanceof CameraPinholeRadial) {
				CameraPinholeRadial radial = (CameraPinholeRadial)parameters;

				ProtoCameraModels.PinholeRadial.Builder pr = ProtoCameraModels.PinholeRadial.newBuilder();

				boofToProto(radial, pr);

				out.println("# Pinhole camera model with radial and tangential distortion");
				out.println("# textual protobuf format");
				out.println("# (fx,fy) = focal length, (cx,cy) = principle point, (width,height) = image shape");
				out.println("# radial = radial distortion, t1,t2) = tangential distortion");
				out.println();
				TextFormat.print(pr,out);
			} else {
				ProtoCameraModels.Pinhole.Builder p = ProtoCameraModels.Pinhole.newBuilder();
				boofToProto(parameters, p);

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

	/**
	 * Saves stereo camera parameters to disk
	 * @param parameters Stereo parameters
	 * @param filePath path to file
	 */
	public static void save(StereoParameters parameters , String filePath ) {
		PrintStream out;
		try {
			out = new PrintStream(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			ProtoCameraModels.StereoRadial.Builder ps = ProtoCameraModels.StereoRadial.newBuilder();
			ProtoCameraModels.PinholeRadial.Builder pl = ps.getLeftBuilder();
			ProtoCameraModels.PinholeRadial.Builder pr = ps.getLeftBuilder();
			ProtoCameraModels.Se3.Builder pr2l = ps.getRightToLeftBuilder();

			boofToProto(parameters.left, pl );
			boofToProto(parameters.right, pr );
			boofToProto(parameters.getRightToLeft(), pr2l );

			ps.setLeft( pl.build() );
			ps.setRight( pr.build() );
			ps.setRightToLeft( pr2l.build() );

			out.println("# Stereo Camera Model");
			out.println("# textual protobuf format");
			out.println();
			TextFormat.print(ps,out);

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

			ProtoCameraModels.Pinhole.Builder builder = ProtoCameraModels.Pinhole.newBuilder();
			TextFormat.merge(reader, builder);

			ProtoCameraModels.Pinhole p = builder.build();

			CameraPinhole model = new CameraPinhole();
			protoToBoof(p, model);

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
			ProtoCameraModels.PinholeRadial.Builder builder = ProtoCameraModels.PinholeRadial.newBuilder();
			TextFormat.merge(reader, builder);

			ProtoCameraModels.PinholeRadial pr = builder.build();

			CameraPinholeRadial model = new CameraPinholeRadial();
			protoToBoof(pr, model);

			return model;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Loads a stereo camera model with radial and tangential distortion from disk
	 * @param reader Input to text
	 * @return The camera model
	 */
	public static StereoParameters loadStereo( Reader reader ) {
		try {
			ProtoCameraModels.StereoRadial.Builder builder = ProtoCameraModels.StereoRadial.newBuilder();
			TextFormat.merge(reader, builder);

			ProtoCameraModels.StereoRadial ps = builder.build();

			StereoParameters param = new StereoParameters();
			param.left = new CameraPinholeRadial();
			param.right = new CameraPinholeRadial();

			protoToBoof(ps.getRightToLeft(), param.rightToLeft);
			protoToBoof(ps.getLeft(), param.left);
			protoToBoof(ps.getRight(), param.right);

			return param;
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

	private static void boofToProto( Se3_F64 boof, ProtoCameraModels.Se3.Builder proto) {
		proto.setTx( boof.T.x );
		proto.setTy( boof.T.y );
		proto.setTz( boof.T.z );

		proto.setR00( boof.R.data[0] );
		proto.setR01( boof.R.data[1] );
		proto.setR02( boof.R.data[2] );

		proto.setR10( boof.R.data[3] );
		proto.setR11( boof.R.data[4] );
		proto.setR12( boof.R.data[5] );

		proto.setR20( boof.R.data[6] );
		proto.setR21( boof.R.data[7] );
		proto.setR22( boof.R.data[8] );
	}

	private static void boofToProto( CameraPinholeRadial radial, ProtoCameraModels.PinholeRadial.Builder pr) {
		ProtoCameraModels.Pinhole.Builder p = ProtoCameraModels.Pinhole.newBuilder();
		boofToProto(radial, p);
		pr.setPinhole(p.build());
		pr.setT1(radial.getT1());
		pr.setT2(radial.getT2());
		if( radial.radial != null ) {
			for (int i = 0; i < radial.radial.length; i++) {
				pr.addRadial(radial.radial[i]);
			}
		}
	}

	private static void boofToProto(CameraPinhole parameters, ProtoCameraModels.Pinhole.Builder p) {
		p.setWidth( parameters.getWidth() );
		p.setHeight( parameters.getHeight() );
		p.setFx( parameters.fx );
		p.setFy( parameters.fy );
		p.setCx( parameters.cx );
		p.setCy( parameters.cy );
		p.setSkew( parameters.skew );
	}

	private static void protoToBoof(ProtoCameraModels.Se3 proto, Se3_F64 boof) {
		boof.T.x = proto.getTx();
		boof.T.y = proto.getTy();
		boof.T.z = proto.getTz();

		boof.R.data[0] = proto.getR00();
		boof.R.data[1] = proto.getR01();
		boof.R.data[2] = proto.getR02();
		boof.R.data[3] = proto.getR10();
		boof.R.data[4] = proto.getR11();
		boof.R.data[5] = proto.getR12();
		boof.R.data[6] = proto.getR20();
		boof.R.data[7] = proto.getR21();
		boof.R.data[8] = proto.getR22();
	}

	private static void protoToBoof(ProtoCameraModels.PinholeRadial pr, CameraPinholeRadial model) {
		protoToBoof(pr.getPinhole(), model);

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
	}

	private static void protoToBoof(ProtoCameraModels.Pinhole p, CameraPinhole model) {
		model.width = p.getWidth();
		model.height = p.getHeight();
		model.skew = p.getSkew();
		model.cx = p.getCx();
		model.cy = p.getCy();
		model.fx = p.getFx();
		model.fy = p.getFy();
	}

}
