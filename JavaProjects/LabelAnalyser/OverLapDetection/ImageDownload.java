import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;

public class ImageDownload {
  public static void main(String args[]) {
    try{
      String USER_AGENT = "Mozilla/5.0";
      String POST_URL = "http://api.labelary.com/v1/printers/12dpmm/labels/4x6/0/";
      String POST_PARAMS = "^XA^FO20,20^GB150,150,5^FS^XZ";
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

    	if (responseCode == HttpURLConnection.HTTP_OK) { //success
        BufferedImage img = ImageIO.read(con.getInputStream());
        ImageIO.write(img, "png", new File("component.png"));
    	} else {
    		System.out.println("POST request not worked");
    	}
    } catch(Exception e) {
      System.out.println(e);
    }
  }
}
