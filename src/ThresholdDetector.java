import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class ThresholdDetector
{
  private ImageProcessor ip;
  private String name;
  
  public ThresholdDetector(ImageProcessor ip, String name)
  {
    this.ip = ip;
    this.name = name;
  }
  
  public void detect(double peakMedian, double background, double ratio)
  {
    int width = this.ip.getWidth();
    int height = this.ip.getHeight();
    ImageProcessor ip2 = new ByteProcessor(width, height);
    if (ratio == 0.0D) {
      ratio = 4.0D;
    }
    double threshold = background + (peakMedian - background) / ratio;
    for (int i = 2; i < width; i++) {
      for (int j = 2; j < height; j++)
      {
        double pixel = this.ip.getPixel(i, j);
        if (pixel > threshold) {
          ip2.putPixel(i, j, 255);
        }
      }
    }
    ImageStack stack = new ImageStack(width, height);
    stack.addSlice("1", ip2);
    ImagePlus imp = new ImagePlus(this.name + " threshold - ratio: " + ratio, stack);
    imp.show();
  }
}
