import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import javafx.util.Pair;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class fontMap {
  public static HashMap<Pair<String,Integer>,Integer> charWidthMap = new HashMap<Pair<String,Integer>,Integer> ();

  public static void buildMap(String fileName) {
    try {
      FileReader reader = new FileReader(fileName);
      BufferedReader bufferReader = new BufferedReader(reader);
      String line = bufferReader.readLine();

      while(line != null) {
        String[] items = line.split(",");
        if(items.length == 4) {
          Integer zplWidth = Integer.parseInt(items[1]);
          Integer actualWidth = Integer.parseInt(items[2]) + Integer.parseInt(items[3]);
          Pair<String,Integer> charWidthPair = new Pair<String,Integer> (items[0], zplWidth);
          charWidthMap.put(charWidthPair, actualWidth);
        } else {
          Integer zplWidth = Integer.parseInt(items[2]);
          Integer actualWidth = Integer.parseInt(items[3]) + Integer.parseInt(items[4]);
          Pair<String,Integer> charWidthPair = new Pair<String,Integer> (",", zplWidth);
          charWidthMap.put(charWidthPair, actualWidth);
        }
        line = bufferReader.readLine();
      }

      bufferReader.close();
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public static void main(String args[]) {
    buildMap("fontMap.txt");
  }
}
