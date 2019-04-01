import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class createImageFromZpl {
    public static final String printerIp = "http://172.27.26.40/printer/";
    public static final String CHARACTER_SET_UTF8 = "UTF-8";

    public static void main(String[] args)  {
        String inputZpl = "^XA^FO523,170^GB172,27^FS^FO939,514^GB122,42^FS^FO259,219^GB567,149^FS^FO494,379^GB229,19^FS^FO81,369^GB116,28^FS^FO91,409^GB566,145^FS^FO19,659^GB1195,3^FS^FO969,879^GB199,79^FS^FO1006,909^GB106,16^FS^FO1000,979^GB122,20^FS^FO399,684^GB440,24^FS^FO408,748^GB272,23^FS^FO402,849^GB416,24^FS^FO340,799^GB157,16^FS^FO340,979^GB215,25^FS^FO621,979^GB228,24^FS^FO46,1044^GB149,24^FS^FO145,1074^GB405,19^FS^FO47,1104^GB171,16^FS^FO56,1134^GB666,112^FS^FO99,799^GB197,197^FS^FO819,1069^GB155,155^FS^FO730,1708^GB358,31^FS^FO99,1409^GB1009,149^FS^FO99,1459^GB1009,4^FS^FO99,1509^GB1009,4^FS^FO149,1409^GB4,149^FS^FO399,1409^GB4,149^FS^FO679,1409^GB4,149^FS^FO879,1409^GB4,149^FS^FO127,1420^GB13,18^FS^FO237,1419^GB82,19^FS^FO475,1419^GB129,19^FS^FO744,1419^GB70,19^FS^FO926,1419^GB146,19^FS^FO121,1471^GB2,0^FS^FO685,1469^GB81,10^FS^FO886,1469^GB119,13^FS^FO49,1564^GB1049,4^FS^FO49,1564^GB4,134^FS^FO199,1564^GB4,134^FS^FO349,1564^GB4,134^FS^FO499,1564^GB4,134^FS^FO649,1564^GB4,134^FS^FO799,1564^GB4,134^FS^FO949,1564^GB4,134^FS^FO1099,1564^GB4,139^FS^FO49,1699^GB1049,4^FS^FO86,1575^GB87,37^FS^XZ";

        try {
            createImage(inputZpl, "output.png");
        } catch (InterruptedException | IOException e) {
            System.err.println("Error in creating image from zpl - " + e.getMessage());
        }
    }

    /**
     * This method is used for creating image from input zpl string
     * @param inputZpl
     * @param filename
     * @throws InterruptedException
     * @throws IOException
     */
    private static void createImage(String data, String filename) throws InterruptedException, IOException  {
        String encodedData = URLEncoder.encode(data, CHARACTER_SET_UTF8);

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
    }

    /**
     * This method is used for obtaining image url from html output
     * @param result
     */
    private static String getUrl(String result) {
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
}
