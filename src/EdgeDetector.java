import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class EdgeDetector
{
  private ImageProcessor ip;
  private String name;
  
  public EdgeDetector(ImageProcessor ip, String name)
  {
    this.ip = ip;
    this.name = name;
  }
  
  public void detect(double peakMedian, double background, double ratio)
  {
    int width = this.ip.getWidth();
    int height = this.ip.getHeight();
    ImageProcessor ip2 = new ByteProcessor(width, height);
    double effectiveRatio;
    if (ratio == 0.0D) {
      effectiveRatio = peakMedian / background / 4.0D;
    } else {
      effectiveRatio = peakMedian / background / ratio;
    }
    for (int i = 2; i < width; i++) {
      for (int j = 2; j < height; j++)
      {
        double pixel = this.ip.getPixel(i, j);
        if (pixel > background) {
          if (pixel >= peakMedian) {
            ip2.putPixel(i, j, 255);
          } else if ((this.ip.getPixel(i - 1, j - 1) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i, j - 1) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i + 1, j - 1) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i - 1, j) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i + 1, j) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i - 1, j - 1) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i, j) * effectiveRatio < pixel) || 
            (this.ip.getPixel(i + 1, j + 1) * effectiveRatio < pixel)) {
            ip2.putPixel(i, j, 255);
          }
        }
      }
    }
    ImageStack stack = new ImageStack(width, height);
    stack.addSlice("1", ip2);
    ImagePlus imp = new ImagePlus(this.name + " edges - ratio: " + ratio, stack);
    imp.show();
  }
}
