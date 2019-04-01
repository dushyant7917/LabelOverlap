import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

public class PixelCoordinates {
  public static void FFF() {
    try {
      BufferedImage img = null;
      File f = null;

      // read image
      f = new File("label-6.png");
      img = ImageIO.read(f);

      // get image width and height
      int width = img.getWidth();
      int height = img.getHeight();

      int INF = 123456789;
      int RMX = -INF;
      int LMX = INF;
      int BMY = -INF;
      int TMY = INF;

      int p, alpha, red, green, blue, avg;

      for(int y = 0; y < height/3; y++){
        for(int x = 0; x < width; x++){
          p = img.getRGB(x,y);
          alpha = (p>>24)&0xff;
          red = (p>>16)&0xff;
          green = (p>>8)&0xff;
          blue = p&0xff;
          avg = (red + green + blue) / 3;

          if(avg == 0) {
            RMX = Math.max(RMX, x);
            LMX = Math.min(LMX, x);
            TMY = Math.min(TMY, y);
            BMY = Math.max(BMY, y);
          }
        }
      }

      System.out.println("Top Left Corner -> " + LMX + "," + TMY);
      System.out.println("Top Right Corner -> " + RMX + "," + TMY);
      System.out.println("Bottom Left Corner -> " + LMX + "," + BMY);
      System.out.println("Bottom Right Corner -> " + RMX + "," + BMY);
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public static void main(String args[]) {
    FFF();
  }
}
