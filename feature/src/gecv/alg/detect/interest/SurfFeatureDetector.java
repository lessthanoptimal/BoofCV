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

package gecv.alg.detect.interest;

import gecv.abst.detect.extract.FeatureExtractor;
import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I16;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
// todo add finer scale detector too
public class SurfFeatureDetector {

	protected FeatureExtractor extractor;

	protected ImageFloat32 intensity[] = new ImageFloat32[3];
	protected int spaceIndex = 0;
	protected QueueCorner foundFeatures = new QueueCorner(100);

	// List of found feature points
	protected List<ScalePoint> foundPoints = new ArrayList<ScalePoint>();

	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.linear(3);
	DenseMatrix64F x = new DenseMatrix64F(3,1);
	DenseMatrix64F y = new DenseMatrix64F(3,1);
	DenseMatrix64F A = new DenseMatrix64F(3,3);

	public SurfFeatureDetector(FeatureExtractor extractor) {
		this.extractor = extractor;
	}

	public void detect( ImageFloat32 integral ) {
		for( int i = 0; i < intensity.length; i++ ) {
			intensity[i] = new ImageFloat32(integral.width,integral.height);
		}

		// todo save previously computed sizes for reuse in higher octaves
		// todo add check to make sure it isn't larger than the image
		detectOctave(integral,1,9,15,21,27);
		detectOctave(integral,2,15,27,39,51);
		detectOctave(integral,4,27,51,75,99);
//		detectOctave(integral,4,9,15,21,27,39,51,75,99);
		// todo automate and add more octaves
	}

	protected void detectOctave( ImageFloat32 integral , int skip , int ...size ) {

		int w = integral.width/skip;
		int h = integral.height/skip;

		for( int i = 0; i < intensity.length; i++ ) {
			intensity[i].reshape(w,h);
		}

		// compute feature intensity in each level
		for( int i = 0; i < size.length; i++ ) {
			// todo don't extract the sign here, do it later
			SurfHessianDetector.intensity(integral,skip,size[i],intensity[spaceIndex]);

			spaceIndex++;
			if( spaceIndex >= 3 )
				spaceIndex = 0;

			// find maximum in scale space
			if( i >= 2 ) {
				findLocalScaleSpaceMax(size,i-1,skip);
			}
		}
	}

	private void findLocalScaleSpaceMax(int []size, int level, int skip) {
		int index0 = spaceIndex;
		int index1 = (spaceIndex + 1) % 3;
		int index2 = (spaceIndex + 2) % 3;

		ImageBorder_F32 inten0 = FactoryImageBorder.value(intensity[index0],0);
		ImageFloat32 inten1 = intensity[index1];
		ImageBorder_F32 inten2 = FactoryImageBorder.value(intensity[index2],0);

		// find local maximums in image 2D space
		foundFeatures.reset();
		extractor.process(intensity[index1],null,-1,null,foundFeatures);

		int levelSize = size[level];
		int sizeStep = levelSize-size[level-1];

		// see if these local maximums are also a maximum in scale-space
		for( int i = 0; i < foundFeatures.num; i++ ) {
			Point2D_I16 f = foundFeatures.get(i);

			float val = inten1.get(f.x,f.y);

//			if( checkMax(inten0,val,f.x,f.y) )
//			if( checkMax(inten2,val,f.x,f.y) )
//			if( checkMax(inten0,val,f.x,f.y) && checkMax(inten2,val,f.x,f.y) )
//				foundPoints.add( new ScalePoint(f.x,f.y,2*1.2*levelSize/9.0));

			// see if it is a max in scale-space too
			if( checkMax(inten0,val,f.x,f.y) && checkMax(inten2,val,f.x,f.y) ) {
			// interpolate the position TODO more details
				computeFirstDeriv(intensity[index0],inten1,intensity[index2],f.x,f.y);
				computeSecondDeriv(intensity[index0],inten1,intensity[index2],f.x,f.y);

				CommonOps.invert(A);
				CommonOps.mult(A,y,x);

				float val0 = (float)x.data[0];
				float val1 = (float)x.data[1];
				float val2 = (float)x.data[2];

				if( Math.abs(val0) <= 0.5f && Math.abs(val1) <= 0.5f && Math.abs(val2) <= 0.5f)
				{
					float interpX = (f.x+val0)*skip;
					float interpY = (f.y+val1)*skip;
					float interpS = levelSize+val2*sizeStep;

					double scale =  1.2*interpS/9.0;
					foundPoints.add( new ScalePoint((int)interpX,(int)interpY,scale));
//				} else {
//					System.out.println("hu");
				}
			}
		}
	}

	/**
	 * Sees if the best score in the current layer is greater than all the scores in a 3x3 neighborhood
	 * in another layer.
	 */
	protected static boolean checkMax(ImageBorder_F32 inten, float bestScore, int c_x, int c_y) {
		for( int y = c_y -1; y <= c_y+1; y++ ) {
			for( int x = c_x-1; x <= c_x+1; x++ ) {
				if( inten.get(x,y) >= bestScore ) {
					return false;
				}
			}
		}
		return true;
	}

	private void computeFirstDeriv( ImageFloat32 inten0 , ImageFloat32 inten1 , ImageFloat32 inten2 ,
									int x , int y )
	{
		float Dx = (inten1.get(x+1,y) - inten1.get(x-1,y))/2.0f;
		float Dy = (inten1.get(x,y+1) - inten1.get(x,y-1))/2.0f;
		float Ds = (inten2.get(x,y) - inten0.get(x,y))/2.0f;

		this.y.data[0] = Dx;
		this.y.data[1] = Dy;
		this.y.data[2] = Ds;
	}

	private void computeSecondDeriv( ImageFloat32 inten0 , ImageFloat32 inten1 , ImageFloat32 inten2 ,
									 int x , int y )
	{
		float middle = inten1.get(x,y);

		float Dxx = inten1.get(x+1,y) + inten1.get(x-1,y) -2*middle;
		float Dyy = inten1.get(x,y+1) + inten1.get(x,y-1) -2*middle;
		float Dss = inten2.get(x,y) + inten0.get(x,y) -2*middle;

		float Dxy = inten1.get(x+1,y+1) - inten1.get(x-1,y+1) - inten1.get(x+1,y-1) + inten1.get(x-1,y-1);
		float Dxs = inten2.get(x+1,y) - inten2.get(x-1,y) - inten0.get(x+1,y) + inten0.get(x-1,y);
		float Dys = inten2.get(x,y+1) - inten0.get(x,y+1) - inten2.get(x,y-1) + inten0.get(x,y-1);

		// todo why does divide by 4 help?
		Dxy /= 4;
		Dxs /= 4;
		Dys /= 4;

		this.A.data[0] = Dxx;
		this.A.data[1] = this.A.data[3] = Dxy;
		this.A.data[2] = this.A.data[6] = Dxs;
		this.A.data[4] = Dyy;
		this.A.data[5] = this.A.data[7] = Dys;
		this.A.data[8] = Dss;
	}

	public List<ScalePoint> getFoundPoints() {
		return foundPoints;
	}
}
