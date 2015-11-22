package fr.main2.microhttpd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;


public class MicroHTTPDActivity extends ActionBarActivity {

    private MicroHTTPD m_micro_httpd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro_httpd);

        TextView greeting_textview = (TextView) findViewById( R.id.greetings );

        CheckBox http_readwrite_checkbox = (CheckBox) findViewById( R.id.http_readwrite_checkbox );
        http_readwrite_checkbox.setChecked(getPreferences(MODE_PRIVATE).getBoolean("HttpReadWrite", false));
        http_readwrite_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                //CheckBox http_readwrite_checkbox = (CheckBox) findViewById(R.id.http_readwrite_checkbox);
                m_micro_httpd.m_readwrite = isChecked;

                if (isChecked)
                    Toast.makeText(getApplicationContext(),
                            "HTTP server now accepts PUT and DELETE requests.",
                            Toast.LENGTH_LONG)
                            .show();
            }
        });

        CheckBox http_secure_checkbox = (CheckBox) findViewById( R.id.http_secure_checkbox );
        http_secure_checkbox.setChecked(getPreferences(MODE_PRIVATE).getBoolean("HttpSecure", false));
        http_secure_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                //CheckBox http_readwrite_checkbox = (CheckBox) findViewById(R.id.http_readwrite_checkbox);
                m_micro_httpd.m_http_secure = isChecked;

                if (isChecked)
                    Toast.makeText(getApplicationContext(),
                            "HTTP server will now uses HTTPs. RESTART NEEDED.",
                            Toast.LENGTH_LONG)
                            .show();
            }
        });

        /*
        Spinner spinner = (Spinner) rootView.findViewById(R.id.http_root_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.http_root_spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        */

        try {
            m_micro_httpd = new MicroHTTPD();
            m_micro_httpd.m_readwrite = getPreferences(MODE_PRIVATE).getBoolean("HttpReadWrite", false);
            m_micro_httpd.m_http_secure = getPreferences(MODE_PRIVATE).getBoolean("HttpSecure", false);
        } catch (Exception e) {
            String msg = "Couldn't create server:\n" + e;
            greeting_textview.setText(msg);
            greeting_textview.setTextColor(0XFF0000);
            Log.e("httpd", msg);
            System.err.println(msg);
            //System.exit(-1);
        }

        try {
            if (m_micro_httpd.m_http_secure) {
                /*
                The trusted public certificates of servers are stored in the cacerts keystore file,
                The trusted public certificates and private keys of clients are stored in the clientcerts keystore file.
                The cacerts keystore file has a default password of changeit;
                The clientcerts keystore file has a default password of passphrase.

                keytool -genkey -keyalg RSA -alias selfsigned -keystore app/src/main/res/raw/keystore_bks.bks -storepass chtitpd -keysize 2048 -validity 3650 -storetype BKS-v1 -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../../../deps/Java/bks/bcprov-jdk15on-153.jar -ext SAN=DNS:localhost,IP:127.0.0.1
                keytool -genkey -keyalg RSA -alias selfsigned -keystore app/src/main/res/raw/keystore_bks.bks -storepass chtitpd -keysize 2048 -validity 3650 -storetype BKS-v1 -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../bcprov-jdk15on-153.jar

                */
                char[] kspass = "chtitpd".toCharArray();

                String kstype = KeyStore.getDefaultType();
                KeyStore ks = KeyStore.getInstance(kstype);
                //KeyStore ks = KeyStore.getInstance("JKS"); // PKCS12 or JKS (Java) or BKS (Android), KeychainStore (Apple)
                //ks.load(new FileInputStream("/Users/kljh/Downloads/Projects/keystore.jks"), kspass);
                //ks.load(new FileInputStream("file:///android_asset/singuler.keystore"), kspass);
                //ks.load(new FileInputStream("file:///Users/kljh/Downloads/Projects/keystore.jks"), kspass);
                //ks.load(getResources().getAssets().open("keystore.jks"), kspass);
                ks.load(getResources().openRawResource(R.raw.keystore_bks), kspass);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, kspass);

                SSLServerSocketFactory ssf = NanoHTTPD.makeSSLSocketFactory(ks, kmf);
                //SSLServerSocketFactory ssf = NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray());

                m_micro_httpd.makeSecure(ssf, new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"});
            }
        } catch (Exception e) {
            String msg = "Couldn't intialise secure socket:\n" + e;
            greeting_textview.setText(msg);
            greeting_textview.setTextColor(0XFF0000);
            Log.e("httpd", msg);
            System.err.println(msg);
            //System.exit(-1);
        }

        try {
            m_micro_httpd.start();
        } catch (Exception e) {
            String msg = "Couldn't start server:\n" + e;
            greeting_textview.setText(msg);
            greeting_textview.setTextColor(0XFF0000);
            Log.e("httpd", msg);
            System.err.println(msg);
            //System.exit(-1);
        }

        try {
            // Copy Resources to ExternalFiles
            File file = new File(getExternalFilesDir(null), "index.html");
            m_micro_httpd.m_homepage = file.getAbsolutePath();
            //if (!file.exists())
            {
                HashMap<String, Integer> deps = new HashMap<String, Integer>();
                deps.put("index.html", R.raw.index_html);
                deps.put("list_js.js", R.raw.list_js);
                deps.put("jquery_min.js", R.raw.jquery_min_js);
                deps.put("handsontable_full_min_js.js", R.raw.handsontable_full_min_js);
                deps.put("handsontable_full_min_css.css", R.raw.handsontable_full_min_css);


                for (Map.Entry<String, Integer> entry : deps.entrySet())
                {
                    //InputStream is = getResources().openRawResource(R.raw.index_html);
                    //OutputStream os = new FileOutputStream(file);
                    InputStream is = getResources().openRawResource(entry.getValue());
                    OutputStream os = new FileOutputStream(new File(getExternalFilesDir(null), entry.getKey()));

                    byte[] data = new byte[is.available()];
                    is.read(data);
                    os.write(data);
                    is.close();
                    os.close();
                }
            }
        } catch (Exception e) {
            Log.w("Resources2External", "Error", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            // if the phone is connected to a Wifi network
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

            // if the phone serves as an Wifi HotSpot, we have to iterate through all the network interfaces
            try {
                if (ipAddress==0)
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    //Log.e("Network interface", intf.getDisplayName());
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        //Log.e("Network address", inetAddress.toString());
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipHostname = inetAddress.getHostAddress();
                            byte b[] = inetAddress.getAddress();
                            // (my_byte & 0xff) converts signed my_byte into a unsigned int
                            if (b.length==4) {
                                Log.e("Network IP", inetAddress.toString());
                                ipAddress = ((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff);
                            } else {
                                Log.e("Network IP v6 / MAC", inetAddress.toString());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("Network exception", ex.toString());
            }
            if (ipAddress==0)
                ipAddress = 1 * 0x1000000 + 127; // little endian
            String httpd_addr = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            final String url = (m_micro_httpd.m_http_secure?"https":"http") + "://" + httpd_addr + ":" + m_micro_httpd.getListeningPort() + "/";
            final String query = "?pict="+Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getCanonicalPath();

            TextView tv = (TextView) findViewById(R.id.http_url);
            tv.setText(url);

            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            int qr_size = Math.min(width,height)>700 ? 512 : 256;

            ImageView iv = (ImageView) findViewById(R.id.http_qr_url);
            Bitmap bmp = makeQRImage(url+query, qr_size);
            if (bmp!=null) iv.setImageBitmap(bmp);
            iv.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            });
        }
        catch( Exception e )
        {
            System.err.println( "Couldn't get server IP:\n" + e );
            System.exit( -1 );
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
        ed.putBoolean("HttpReadWrite", m_micro_httpd.m_readwrite);
        ed.putBoolean("HttpSecure", m_micro_httpd.m_http_secure);
        ed.commit();
    }


    private static Bitmap makeQRImage(String txt, int qr_size) {

        try {

            //Map<EncodeHintType, ?> zxing_hints = new Map<EncodeHintType, ?>();
            //zxing_hints.put( EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q );

            MultiFormatWriter zxing_writer = new MultiFormatWriter();
            BitMatrix zxing_bit_matrix = zxing_writer.encode(txt, BarcodeFormat.QR_CODE, qr_size, qr_size); //, zxing_hints),

            int height = zxing_bit_matrix.getHeight();
            int width = zxing_bit_matrix.getWidth();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
            for (int x = 0; x < width; x++)
                for (int y = 0; y < height; y++)
                    bmp.setPixel(x, y, zxing_bit_matrix.get(x,y) ? Color.BLACK : Color.TRANSPARENT);

            return bmp;
        } catch (Exception e) {
            System.out.println("failure: " + e);
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_micro_httpd, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


class MicroHTTPD extends NanoHTTPD {
    public boolean m_readwrite = false;
    public boolean m_http_secure = false;
    public String m_homepage;

    public MicroHTTPD() throws IOException {
        super(8080);
        super.MIME_TYPES = this.MIME_TYPES;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Map<String, String> params = session.getParms(); // parsed query
        String query = session.getQueryParameterString(); // raw query
        String uri = session.getUri(); // excl query
        Method verb = session.getMethod(); // GET, POST, PUT, etc.

        File f = new File(uri).getAbsoluteFile();

        String msg = "";
        if (verb == Method.OPTIONS) {
            // OPTIONS (to ask permission to GET, POST, PUT)
            NanoHTTPD.Response res = createResponse(Response.Status.OK, "text/plain", "");
            res.addHeader("Access-Control-Allow-Origin", "*");
            res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT");
            res.addHeader("Access-Control-Allow-Headers", "*");
            return res;
        } else if (verb == Method.PUT) {
            // PUT
            if (!m_readwrite)
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: HTTP server set to read-only mode.");

            try {
                //BufferedReader reader = new BufferedReader(new InputStreamReader(session.getInputStream()));
                //String line;
                //while ((line = reader.readLine()) != null)
                //    msg += line + "\n";
                //session.get

                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write(msg, 0, msg.length());
                msg = "written " + msg.length();
                return createResponse(Response.Status.OK, "text/plain", msg);
            } catch (Exception e) {
                msg += "error while writing file " + uri;
                return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", msg);
            }
        } else if (verb == Method.DELETE) {
            // DELETE
            if (!m_readwrite)
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: HTTP server set to read-only mode.");
            return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: HTTP DELETE not implemented.");
        } else {
            if (f.exists()) {
                if (!f.isDirectory()) {
                    /*
                    // initial dummy implementation
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(f));
                        String line;
                        while ((line = reader.readLine()) != null)
                            msg += line + "\n";
                        return createResponse(Response.Status.OK, "text/plain", msg); // !!
                    } catch (Exception e) {
                        msg += "error while reading file " + uri;
                        return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", msg);
                    }
                    */

                    String mimeTypeForFile = getMimeTypeForFile(uri);
                    return serveFile(uri, headers, f, mimeTypeForFile);
                } else {
                    // folder

                    if (params.containsKey("list")) {
                        // list directory

                        boolean calculate_hashes = false;
                        if (params.containsKey("hash"))
                            calculate_hashes = Boolean.parseBoolean(params.get("hash"));
                        return listDirectory(uri, f, calculate_hashes);
                    } else if (new File(f.getAbsolutePath()+"/index.html").exists()) {

                        String mimeTypeForFile = getMimeTypeForFile(uri);
                        return serveFile(uri, headers, new File(f.getAbsolutePath() + "/index.html"), mimeTypeForFile);

                    } else if (uri.equals("/")) {
                        Response res = createResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML,
                                "<html><body>Redirected: <a href=\"" + m_homepage + "\">" + m_homepage + "</a></body></html>");
                        res.addHeader("Location", m_homepage);
                        return res;
                    }
                }
            } else {
                // does not exist

            }
            return createResponse(Response.Status.NOT_FOUND, "text/plain", uri + " not found");
        }
    }

    // Serves file. Uses only URI, ignores all headers and HTTP parameters.
    protected Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, fis, (int)dataLen);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime, new FileInputStream(file), (int)file.length());
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }


    /*
    private Response respondFolder(String uri, Map<String, String> headers, IHTTPSession session) {
        String homeDir = "";

        // Browsers get confused without '/' after the directory, send a redirect.
        File f = new File(homeDir, uri);
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = createResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML,
                    "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }
    }
    */

    protected Response listDirectory(String uri, File f, boolean calculate_hashes) {

        List<String> directories = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        }));
        Collections.sort(directories);

        List<String> files = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        }));
        Collections.sort(files);


        JSONArray ls = new JSONArray();

        try {
            for (String directory : directories) {
                JSONObject item = new JSONObject();
                item.put("name", directory + "/");
                ls.put(item);
            }

            for (String file : files) {
                File fi = new File(f, file);

                JSONObject item = new JSONObject();
                item.put("name", file);
                item.put("length", fi.length());
                item.put("lastModified", fi.lastModified());
                if (calculate_hashes) {
                    item.put("file_handler_hash", fi.hashCode());
                    item.put("md5_hash", file_md5(fi));
                }
                ls.put(item);
            }

            String json_string = ls.toString(4);
            return createResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, json_string);

        } catch (JSONException e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.IStatus status, String mimeType, InputStream message, int length) {
        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message, length);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "*");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType, String message) {
        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "*");
        return res;
    }

    // Get MIME type from file name extension, if possible
    /* now part of NanoHttp
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? "application/octet-stream" : mime;
    }
    */

    // Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("html", "text/html");
        put("htm", "text/html");
        put("xml", "text/xml");
        put("css", "text/css");
        put("txt", "text/plain");
        put("md", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpeg", "image/jpeg");
        put("jpg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("ogg", "application/x-ogg");
    }};

    private static final String file_md5(File f) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance(MD5);

            // Input stream
            InputStream is = new FileInputStream(f);

            // Input stream with digest on the fly
            DigestInputStream ish = new DigestInputStream(is, digest);
            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = ish.read(buffer);
                //digest.update(s.getBytes());
            } while (numRead != -1);
            ish.close();

            MessageDigest digested = ish.getMessageDigest();
            byte mdBytes[] = digested.digest();
            byte mdBytes2[] = digest.digest();


            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : mdBytes) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}