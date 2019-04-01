import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Pair;

class Field {
  public int LMX, RMX, TMY, BMY;
  public String fieldType, zpl;
}

public class OverLap {
  public static List<String> commands = null;
  public static List<Field> fields = new ArrayList<Field>();
  public static int X, Y;
  public static int W, H;
  public static int LMX, RMX, TMY, BMY;
  public static int counter = 0;
  public static StringBuffer gridZPL = new StringBuffer("^XA");
  public static char orientation = 'N';
  public static String printerIp = "http://172.27.26.40/printer/";
  public static String CHARACTER_SET_UTF8 = "UTF-8";
  public static long startTime;
  public static long curTime;
  public static double executionTime;
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

  public static String fieldType() {
    boolean FD = false;
    boolean A0 = false;

    for(String command : commands) {
      if(command.length()>=3 && command.substring(0,3).equals("^GB")) return "GraphicBox";
      if(command.length()>=2 && command.substring(0,2).equals("^B")) return "BarCode|QRCode";
      if(command.length()>=3 && command.substring(0,3).equals("^XG")) return "Image";
      if(command.length()>=3 && command.substring(0,3).equals("^FD")) FD = true;
      if(command.length()>=3 && command.substring(0,3).equals("^A0")) A0 = true;
    }

    if(A0 && FD) return "Text";

    return "Unknown";
  }

  public static String base64ToZPL(String encodedStream) {
    Base64.Decoder decoder = Base64.getDecoder();
    String decodedStream = new String(decoder.decode(encodedStream));

    return decodedStream;
  }

  public static void zplToFile(String filename, String data) {
    try {
      FileWriter writer = new FileWriter(filename);
      BufferedWriter buffer = new BufferedWriter(writer);
      buffer.write(data);
      buffer.close();
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public static void preProcess(String filename, String commandFile) {
    try {
      X = 0;
      Y = 0;
      FileReader reader = new FileReader(filename);
      BufferedReader bufferReader = new BufferedReader(reader);
      FileWriter writer = new FileWriter(commandFile);
      BufferedWriter bufferWriter = new BufferedWriter(writer);
      String line = bufferReader.readLine();
      while (line != null) {
        if(line.length()==0 ||
          line.charAt(0) == ' ' ||
          line.charAt(0) == '\t' ||
          line.charAt(0) == '\n' ||
          (line.length()>=3 && line.substring(0,3).equals("^FX")) ||
          (line.length()>=3 && line.substring(0,3).equals("^XA")) ||
          (line.length()>=4 && line.substring(0,4).equals("^MCY")) ||
          (line.length()>=3 && line.substring(0,3).equals("^XZ"))) {
          // if any of the above condition is true then skip this line
        } else if(line.length()>=3 && line.substring(0,3).equals("^LH")){
          X = Integer.parseInt(String.valueOf(line.charAt(3)));
          Y = Integer.parseInt(String.valueOf(line.charAt(5)));
        } else {
          bufferWriter.write(line + "\n");
        }
				line = bufferReader.readLine();
			}
      bufferReader.close();
      bufferWriter.close();
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public static void deleteFile(String filename) {
    File file = new File(filename);

    if(file.delete()){
      System.out.println("********* File " + filename + " deleted! *********");
    } else {
      System.out.println("********* File " + filename + " doesn't exist! *********");
    }
  }

  public static String getUrl(String result) {
      String pattern = "(.*)<IMG SRC=\"(.*)&";
      Pattern r = Pattern.compile(pattern);
      Matcher m = r.matcher(result);
      String out = "";
      if (m.find()) {
          out = m.group(2);
      }
      String url = printerIp + out + "&otype=PNG";
      return url;
  }

  public static void zplToPNGusingPrinter(String componentZPL, String filename) {
    try {
      String encodedData = URLEncoder.encode(componentZPL, CHARACTER_SET_UTF8);

      System.out.println("Making a call to printer " + printerIp + "zpl");
      String[] command = {"curl", "-d" ,"data="+encodedData+"&dev=R&oname=UNKNOWN&otype=ZPL&prev=Preview+Label&pw=", printerIp + "zpl"};

      System.out.println("Getting the image url from printer..");
      ProcessBuilder process = new ProcessBuilder(command);
      Process p = process.start();
      BufferedReader reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
      StringBuilder builder = new StringBuilder();
      String line;
      while ( (line = reader.readLine()) != null) {
          builder.append(line);
          builder.append(System.getProperty("line.separator"));
      }
      String result = builder.toString();
      p.destroy();

      String urlString = getUrl(result);
      System.out.println("Image URL ::" + urlString);
      String[] command1 = {"curl", urlString};

      System.out.println("Retrieving the image from the printer..");
      File stdoutFile = new File(filename);
      process = new ProcessBuilder(command1);
      process.redirectOutput(stdoutFile);
      p = process.start();
      p.waitFor();
      p.destroy();
      System.out.println("Image saved successfully");
    } catch(Exception e) {
      System.out.println(e);
    }
  }

  public static void zplToPNGusingAPI(String componentZPL, String filename) {
    try{
      String USER_AGENT = "Mozilla/5.0";
      String POST_URL = "http://api.labelary.com/v1/printers/12dpmm/labels/4x6/0/";
      String POST_PARAMS = componentZPL;
      String CONTENT_TYPE = "application/x-www-form-urlencoded";

      URL obj = new URL(POST_URL);
    	HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    	con.setRequestMethod("POST");
    	con.setRequestProperty("User-Agent", USER_AGENT);
      con.setRequestProperty("Content-Type", CONTENT_TYPE);

    	// For POST only - START
    	con.setDoOutput(true);
    	OutputStream os = con.getOutputStream();
    	os.write(POST_PARAMS.getBytes());
    	os.flush();
    	os.close();
    	// For POST only - END

    	int responseCode = con.getResponseCode();
    	System.out.println("POST Response Code :: " + responseCode);

    	if(responseCode == HttpURLConnection.HTTP_OK) { //success
        BufferedImage img = ImageIO.read(con.getInputStream());
        ImageIO.write(img, "png", new File(filename));
    	} else {
    		System.out.println("POST request not worked");
    	}

    } catch(Exception e) {
      System.out.println(e);
    }
  }

  public static void getCoordinates() {
    try {
      BufferedImage img = null;
      File f = null;

      // read image
      f = new File("component.png");
      img = ImageIO.read(f);

      // get image width and height
      int width = img.getWidth();
      int height = img.getHeight();

      int INF = 12345678;
      RMX = -INF;
      LMX = INF;
      BMY = -INF;
      TMY = INF;

      int p, alpha, red, green, blue, avg;

      for(int y = 0; y < height; y++){
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

      W = RMX - LMX;
      H = BMY - TMY;

      /*
      System.out.println("Top Left Corner -> " + LMX + "," + TMY);
      System.out.println("Top Right Corner -> " + RMX + "," + TMY);
      System.out.println("Bottom Left Corner -> " + LMX + "," + BMY);
      System.out.println("Bottom Right Corner -> " + RMX + "," + BMY);
      System.out.println("W : " + W);
      System.out.println("H : " + H);
      System.out.println("***********************\n");
      */
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public static void processComponent() {
    Boolean FO = false;
    Boolean FT = false;
    Boolean FB = false;
    int fieldBlockWidth, fieldBlockLines;
    Field field = new Field();
    int indOfComma;
    String tmp;
    StringBuffer componentZPL = new StringBuffer("^XA");

    for(String command : commands) {
      if(command.length()>=3 && command.substring(0,3).equals("^FB")) {
        FB = true;
        String[] params = command.split(",");
        if(params.length >= 2) {
          fieldBlockWidth = Integer.parseInt(params[0].substring(3,params[0].length()));
          System.out.println("FB width : " + fieldBlockWidth);
        }
      }
      if(command.length()>=3 && command.substring(0,3).equals("^FO")) FO = true;
      if(command.length()>=3 && command.substring(0,3).equals("^FT")) FT = true;
      if(command.length()>=4 && command.substring(0,3).equals("^FW")) orientation = command.charAt(3);
      if(command.length()>=3 && command.substring(0,3).equals("^FS")) componentZPL.append("^FW" + orientation);
      componentZPL.append(command);
    }

    componentZPL.append("^XZ");

    // form png for individual component
    startTime = System.nanoTime();
    zplToPNGusingAPI(componentZPL.toString(), "component.png");
    curTime = System.nanoTime();
    executionTime = (curTime - startTime) / 1000000000.0;
    //System.out.println("zpl to png conversion : " + executionTime);

    try {
      getCoordinates();
    } catch(Exception e) {
      System.out.println("|||||||||||||||||||");
    }

    //System.out.println("ZPL : " + componentZPL);

    field.RMX = X + RMX;
    field.LMX = X + LMX;
    field.BMY = Y + BMY;
    field.TMY = Y + TMY;
    field.fieldType = fieldType();
    field.zpl = componentZPL.toString();

    if(field.fieldType.equals("Unknown") == false && H>=0 && W>=0) {
      fields.add(field);
      if(FO == true) gridZPL.append("^FO" + field.LMX + "," + field.TMY + "^GB" + W + "," + H + "^FS");
      else if(FT == true) gridZPL.append("^FT" + field.LMX + "," + field.BMY + "^GB" + W + "," + H + "^FS");
      else {}
    }
  }

  public static void processZPL(String filename) {
    commands = new ArrayList<String>();

    try {
      FileReader reader = new FileReader(filename);
      BufferedReader bufferReader = new BufferedReader(reader);
      String line = bufferReader.readLine();
      while(line != null) {
        // process command
        commands.add(line);

        if(line.length()>=3 && line.substring(0,3).equals("^FS")) {
          processComponent();
          commands = new ArrayList<String>();
          counter++;
          //if(counter == 5) break;
        }

        line = bufferReader.readLine();
			}
      bufferReader.close();
    } catch(IOException e) {
      System.out.println(e);
    }

    gridZPL.append("^XZ");
    zplToPNGusingAPI(gridZPL.toString(), "grid.png");
    //System.out.println();
    //System.out.println("grid zpl : " + gridZPL.toString());
  }

  public static Boolean componentOverlap(Field f1, Field f2) {
    // one component is completely on left side of other
    if(f1.LMX >= f2.RMX || f2.LMX >= f1.RMX) return false;

    // one component is completely above another
    if(f1.TMY >= f2.BMY || f2.TMY >= f1.BMY) return false;

    // Edge Case 1
    // one component is GraphicBox and other component completely lies inside GraphicBox
    if(f1.fieldType.equals("GraphicBox") && f2.RMX<f1.RMX && f2.LMX>f1.LMX && f2.BMY<f1.BMY && f2.TMY>f1.TMY) return false;
    if(f2.fieldType.equals("GraphicBox") && f1.RMX<f2.RMX && f1.LMX>f2.LMX && f1.BMY<f2.BMY && f1.TMY>f2.TMY) return false;

    // Edge Case 2
    // GraphicBox over GraphicBox is sometimes part of label design
    if(f1.fieldType.equals("GraphicBox") && f2.fieldType.equals("GraphicBox")) return false;

    return true;
  }

  public static Boolean detectOverlap() {
    for(int i = 0; i < fields.size(); i++) {
      for(int j = 0; j < fields.size(); j++) {
        if(i == j) continue;
        if(componentOverlap(fields.get(i), fields.get(j))) {
          System.out.println("!!!!!!!!!!!!!!!!!!!!!");
          System.out.println(fields.get(i).fieldType + " | " + fields.get(j).fieldType);
          System.out.println(fields.get(i).zpl);
          System.out.println();
          System.out.println(fields.get(j).zpl);
          System.out.println("!!!!!!!!!!!!!!!!!!!!!");
          return true;
        }
      }
    }

    return false;
  }

  public static void main(String args[]) {
    // build cache to store width for a specific char of specific zpl width
    buildMap("fontMap.txt");

    // base64 stream to ZPL stream
    String zplStream = base64ToZPL("CiAgICBeRlggU3RhcnQgb2YgYSBuZXcgbGFiZWwtc2hlZXQKICAgIF5YQV5NQ1leWFoKCgogXkZYIFNoaXBtZW50IElEICAgICA6IDI2MjEwMzgxMjY1MjAyIF5GUwogXkZYIFNoaXBNZXRob2QgICAgICA6IEFNWk5fSVRfUFJJTUUgXkZTCgpeWEEKICAgICAgICAgICAgICAgICAgICAgICAgXkxIMCwwCiAgICAgIAoKIF5GTzMwLDMwXkEwNjAsNjAKXkZCNDAwLDcsLEwsCl5GRE1YUDVeRlMKCl5GTzUwMCwzMF5HQjQ1MCw5MCwzXkZTCl5GTzUwNSw0NSBeQTAsNjAsNjBeRkIyNjUsMSwwLEwsMF5GRDAuOCBLZ3NeRlMKXkZPNzcwLDMwIF5HQjMsOTAsM15GUwpeRk8gNzc1LDQ1IF5GQjE0NSwxLDAsUiwwXkEwLDYwLDYwXkZEMTEvMDJeRlMKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF5DSTI4Cl5GTzgwLDIwMF5BMDUwLDUwXkZCMTEwMCwyLDIsTCwgXkZIX15GRCBfNERfNjlfNjNfNjhfNjVfNkNfNjVfMjBfNDdfNzJfNjVfNjNfNkZfNUNfMjYgXkZTCl5GTzgwLDMxMF5BMDcwLDcwXkZCMTEwMCwyLDIsTCwgXkZIX15GRF81Nl82OV82MV8yMF82NF82NV82Q182Q182MV8yMF80RF82Rl83M182M182Rl83Nl82MV8yMF8zNF8zN181Q18yNl5GUwpeRk84MCw0NjBeQTA1MCw1MF5GQjExMDAsNCwyLEwsIF5GSF9eRkRfNTZfNjlfNjFfMjBfNjRfNjVfNkNfNkNfNjFfMjBfNERfNkZfNzNfNjNfNkZfNzZfNjFfMjBfMzRfMzdfNUNfMjYgIF8zMl8zMF8zMV8zMl8zMSBfNERfNjlfNkNfNjFfNkVfNkYgXzJDIF80RF82OV82Q182MV82RV82RiBfNDlfNzRfNjFfNkNfNjlfNjVeRlMKXkNJMAoKXkZPMCw3MDBeR0IxMjAwLDUsNV5GUwogICAgCSAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBeRk80MCwxMDMwXkdCNjIwLDEwMCwxMDBeRlMKXkZPNjYwLDEwMzBeR0I1MDAsMTAwLDVeRlMgICAgICAgIAoKXkEwLDEwMCwxMDBeRk82NzAsMTA0NSBeRkI0OTAsMSwwLEMsMCBeRkRDWUNMRV8xXkZTCl5BMCw5MCw5MF5GTzUwLDEwNDUgXkZCNjIwLDEsMCxDLDAgXkZSIF5GRERMTzFeRlMKICAJCl5GTzMwMCw4NTAgXkEwLDgwLDgwIF5GREZERTExODY1MDc1M15GUwoKXkZPODUwLDc0MApeQlhOLDEyLDIwMCwyMiwyMiwsKgpeRkRGREUxMTg2NTA3NTNeRlMKCl5GTzAsMTIzMF5HQjEyMDAsNSw1XkZTCl5CWTQsMy4wLDIxMF5GTzE4MCwxMjcwXkJDLCxOLCwsXkZEPjpTRFY4bGxxV1JKXzAwMV92XkZTCl5GTzAsMTQ5MF5BME4sMzBeRkIxMjAwLDEsMCxDLDBeRkRTRFY4bGxxV1JKXzAwMV92XkZTCl5GTzAsMTU0NV5HQjEyMDAsMSw1XkZTCl5GTzMzMCwxNTQ1XkdCMSwyNDAsMV5GUwpeRk82MjAsMTU0NV5HQjEsMjQwLDFeRlMKXkZPOTEwLDE1NDVeR0IxLDI0MCwxXkZTCl5GTzAsMTc4MF5HQjEyMDAsMSw1XkZTCgogICAgICAgICAgICAgICB0cnVlCiAgICAgICAgICAgICAgICB0cnVlCgogICAgICAgICAgIF5GTzUwLDE1NTUgXkEwLDY1LDY1XkZCMzIwLDEsLENeRkRGQ08xXkZTCiAgICAgICAgICAgICAgICAgICAgICAgICAgIHRydWUKICAgICAgICAgICAgICAgICAgICAgICAgIF5GTzUwLDE2MjAgXkdCODAsODAsODBeRlMKICAgICAgICAgICAgICAgICAgICAgICAgXkZPNTAsMTYzMCBeQTAsODAsODBeRkI4MCwxLCxDXkZSXkZEQV5GUwogICAgICAgICAgICAgICAgICAgICAgICBeRk8xMzAsMTYzMCBeQTAsODAsODBeRkIyMDAsMSwsTF5GUl5GRExPMV5GUwogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKXlhaCg==");

    // below step helps when we write ZPL to a file(whenever a '^' char occurs data is written at a new line in file)
    zplStream = zplStream.replace("^", "\n^");

    // write ZPL to file
    zplToFile("temporary.txt", zplStream);

    // after below step each line of file will contain a single zpl command
    preProcess("temporary.txt", "zpl.txt");

    // delete the temporary/auxilary file which was used for pre-processing of data
    deleteFile("temporary.txt");

    // extract coordinates of individual components from ZPL commands
    processZPL("zpl.txt");

    // returns true and false
    if(detectOverlap()) {
      System.out.println("Overlap present in Label!");
    } else {
      System.out.println("Label OK!");
    }
  }
}
