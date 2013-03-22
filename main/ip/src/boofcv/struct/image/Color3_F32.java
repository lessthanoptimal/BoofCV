package boofcv.struct.image;

/**
 * Stores the values of a 3-band color using floating point numbers.
 *
 * @author Peter Abeles
 */
public class Color3_F32 {
   public int band0;
   public int band1;
   public int band2;

   public void setAll( int value ) {
      band0 = value;
      band1 = value;
      band2 = value;
   }
}
