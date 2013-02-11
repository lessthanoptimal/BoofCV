/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.sfm.d3;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 *
 * @author
 */
public class VideoSequenceSimulator<I extends ImageSingleBand> {

	protected int width,height;

	Random rand = new Random(1234);

	IntrinsicParameters intrinsic;
	DenseMatrix64F K;

	BufferedImage workImage;
	I outputImage;

	List<Square> squares = new ArrayList<Square>();

	public VideoSequenceSimulator( int width , int height, Class<I> inputType ) {

		this.width = width;
		this.height = height;
		workImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_BGR);
		outputImage = GeneralizedImageOps.createSingleBand(inputType,workImage.getWidth(),workImage.getHeight());
	}

	public void setIntrinsic( IntrinsicParameters param ) {
		this.intrinsic = param;
		K = PerspectiveOps.calibrationMatrix(param,null);
	}

	protected void createSquares( int total , double minZ, double maxZ ) {
		squares.clear();

		double t = 0.1;

		for( int i = 0; i < total; i++ ) {
			double z = rand.nextDouble()*(maxZ-minZ)+minZ;
			double x = rand.nextDouble()*2-0.5;
			double y = rand.nextDouble()*2-0.5;

			Square s = new Square();
			s.a.set(x  ,y  ,z);
			s.b.set(x + t, y, z);
			s.c.set(x + t, y + t, z);
			s.d.set(x, y + t, z);

		    s.gray = rand.nextInt(255);

			squares.add(s);
		}

		// sort by depth so that objects farther way are rendered first and obstructed by objects closer in view
		Collections.sort(squares,new Comparator<Square>() {
			@Override
			public int compare(Square o1, Square o2) {
				if( o1.a.z < o2.a.z )
					return -1;
				if( o1.a.z > o2.a.z )
					return 1;
				else
					return 0;
			}
		});
	}

	public I render( Se3_F64 worldToCamera ) {
		Graphics2D g2 = workImage.createGraphics();

		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,workImage.getWidth(),workImage.getHeight());

		for( Square s : squares ) {
			Point2D_F64 p1 = PerspectiveOps.renderPixel(worldToCamera,K,s.a);
			Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCamera,K,s.b);
			Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCamera,K,s.c);
			Point2D_F64 p4 = PerspectiveOps.renderPixel(worldToCamera,K,s.d);

			Polygon p = new Polygon();
			p.addPoint((int)p1.x,(int)p1.y);
			p.addPoint((int)p2.x,(int)p2.y);
			p.addPoint((int)p3.x,(int)p3.y);
			p.addPoint((int)p4.x,(int)p4.y);

			g2.setColor( new Color(s.gray,s.gray,s.gray) );
			g2.fillPolygon(p);
		}

		// TODO apply lense distortion
		ConvertBufferedImage.convertFrom(workImage,outputImage);
		return outputImage;
	}



	private static class Square
	{
		int gray;
		public Point3D_F64 a = new Point3D_F64();
		public Point3D_F64 b = new Point3D_F64();
		public Point3D_F64 c = new Point3D_F64();
		public Point3D_F64 d = new Point3D_F64();
	}
}
