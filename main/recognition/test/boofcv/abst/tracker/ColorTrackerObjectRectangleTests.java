package boofcv.abst.tracker;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Tests for trackers which use color information alone to track an object
 *
 * @author Peter Abeles
 */
public abstract class ColorTrackerObjectRectangleTests extends GenericTrackerObjectRectangleTests<MultiSpectral<ImageUInt8>> {

	MultiSpectral<ImageUInt8> original = new MultiSpectral<ImageUInt8>(ImageUInt8.class,width,height,3);

	boolean multiColor;

	public ColorTrackerObjectRectangleTests( boolean multiColor ) {
		super(new ImageType<MultiSpectral<ImageUInt8>>(ImageType.Family.MULTI_SPECTRAL, ImageDataType.U8,3));

		this.multiColor = multiColor;

		input = new MultiSpectral<ImageUInt8>(ImageUInt8.class,width,height,3);
	}

	@Override
	protected void render( double scale , double tranX , double tranY ) {
		// each region in the target region will have a different color.  Allowing scale, translation, and rotation
		// to be estimated using color information alone

		Quadrilateral_F64 q = initRegion.copy();

		// scale it down a bit so that there is a border
		if( multiColor )
			scale(q,0.95);

		Point2D_F64 ab = average(q.a,q.b);
		Point2D_F64 bc = average(q.b,q.c);
		Point2D_F64 cd = average(q.c,q.d);
		Point2D_F64 da = average(q.d,q.a);
		Point2D_F64 abcd = average(ab,cd);

		Quadrilateral_F64 r0 = new Quadrilateral_F64(q.a,ab,abcd,da,true);
		Quadrilateral_F64 r1 = new Quadrilateral_F64(ab,q.b,bc,abcd,true);
		Quadrilateral_F64 r2 = new Quadrilateral_F64(abcd,bc,q.c,cd,true);
		Quadrilateral_F64 r3 = new Quadrilateral_F64(da,abcd,cd,q.d,true);

		Polygon2D_I32 region[] = new Polygon2D_I32[4];
		region[0] = setPolygon(r0);
		region[1] = setPolygon(r1);
		region[2] = setPolygon(r2);
		region[3] = setPolygon(r3);

		int band0[] = new int[]{100,50,176,0};
		int band1[] = new int[]{150,200,240,40};
		int band2[] = new int[]{20,234,176,210};

		GImageMiscOps.fill(original,0);
		GImageMiscOps.fill(input,0);

		for( int i = 0; i < 4; i++ ) {

			int colorIndex;
			if( multiColor )
				colorIndex = i;
			else
				colorIndex = 0;

			TextureGrayTrackerObjectRectangleTests.convexFill(region[i],original.getBand(0),band0[colorIndex]);
			TextureGrayTrackerObjectRectangleTests.convexFill(region[i],original.getBand(1),band1[colorIndex]);
			TextureGrayTrackerObjectRectangleTests.convexFill(region[i],original.getBand(2),band2[colorIndex]);
		}

		DistortImageOps.affine(original,input, TypeInterpolate.BILINEAR,scale,0,0,scale,tranX,tranY);
	}

	private Point2D_F64 average( Point2D_F64 a , Point2D_F64 b ) {
		return new Point2D_F64((a.x+b.x)/2.0,(a.y+b.y)/2.0);
	}

	private Polygon2D_I32 setPolygon( Quadrilateral_F64 q ) {
		q = q.copy();

		Polygon2D_I32 p = new Polygon2D_I32(4);
		p.vertexes.data[0].set((int)q.a.x,(int)q.a.y);
		p.vertexes.data[1].set((int)q.b.x,(int)q.b.y);
		p.vertexes.data[2].set((int)q.c.x,(int)q.c.y);
		p.vertexes.data[3].set((int)q.d.x,(int)q.d.y);

		return p;
	}

	private void scale( Quadrilateral_F64 q , double scale ) {
		Point2D_F64 center0 = average(average(q.a, q.b), average(q.c, q.d));

		q.a.x *= scale;
		q.a.y *= scale;
		q.b.x *= scale;
		q.b.y *= scale;
		q.c.x *= scale;
		q.c.y *= scale;
		q.d.x *= scale;
		q.d.y *= scale;

		Point2D_F64 center1 = average(average(q.a,q.b),average(q.c,q.d));

		q.a.x += center1.x - center0.x;
		q.a.y += center1.x - center0.x;
		q.b.x += center1.x - center0.x;
		q.b.y += center1.x - center0.x;
		q.c.x += center1.x - center0.x;
		q.c.y += center1.x - center0.x;
		q.d.x += center1.x - center0.x;
		q.d.y += center1.x - center0.x;

	}


}
