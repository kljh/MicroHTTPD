package fr.main2.microhttpd;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;


public class MicroHTTPDActivity extends ActionBarActivity {

    private MicroHTTPD m_micro_httpd;

    /*
    private final IntentFilter m_p2p_intentFilter = new IntentFilter();
    private WifiP2pManager m_p2p_manager;
    private WifiP2pManager.Channel m_p2p_channel;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);  // Remove title bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_micro_httpd);

        TextView greeting_textview = (TextView) findViewById( R.id.greetings );

        final boolean RESTART_ON_CHANGE = true;
        final boolean NO_RESTART_ON_CHANGE = false;
        sync_setting_checkbox("http_secure", R.id.http_secure, MicroHTTPD.http_secure_default,
                "HTTP server will uses HTTPs.\nCertificate is self-signed. The browser will issue a secrurity warning (still exposed to man-in-the-middle attacks).",
                "HTTP server will uses standard HTTP.", RESTART_ON_CHANGE);
        sync_setting_checkbox("http_allow_put", R.id.http_allow_put, MicroHTTPD.http_allow_put_default, null, null, NO_RESTART_ON_CHANGE);
        sync_setting_checkbox("http_allow_delete", R.id.http_allow_delete, MicroHTTPD.http_allow_delete_default, "DELETE requests now allowed", null, NO_RESTART_ON_CHANGE);
        sync_setting_checkbox("http_auth_get", R.id.http_auth_get, MicroHTTPD.http_auth_get_default, null, null, NO_RESTART_ON_CHANGE);
        sync_setting_checkbox("http_auth_put", R.id.http_auth_put, MicroHTTPD.http_auth_put_default, null, null, NO_RESTART_ON_CHANGE);
        sync_setting_checkbox("http_auth_delete", R.id.http_auth_delete, MicroHTTPD.http_auth_delete_default, null, "DELETE requests now allowed without authentification", NO_RESTART_ON_CHANGE);

        String http_password = getPreferences(MODE_PRIVATE).getString("http_password", null);
        if (http_password==null || http_password.equals(""))
            http_password = random_password(8);
        final EditText http_password_input = (EditText) findViewById( R.id.http_password );
        http_password_input.setText(http_password);
        http_password_input.setOnKeyListener(new EditText.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                m_micro_httpd.m_http_password = http_password_input.getText().toString();
                getPreferences(MODE_PRIVATE).edit().putString("http_password", m_micro_httpd.m_http_password).commit();
                return false;
            }
        });


        try {
            m_micro_httpd = new MicroHTTPD();
            m_micro_httpd.m_preferences = getPreferences(MODE_PRIVATE);
            m_micro_httpd.m_http_password = http_password;
        } catch (Exception e) {
            String msg = "Couldn't create server:\n" + e;
            greeting_textview.setText(msg);
            greeting_textview.setTextColor(0XFF0000);
            Log.e("httpd", msg);
            System.err.println(msg);
            //System.exit(-1);
        }

        boolean http_secure = getPreferences(MODE_PRIVATE).getBoolean("http_secure", MicroHTTPD.http_secure_default);
        try {
            if (http_secure) {
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

                String[] supported_secure_protocols = ((SSLServerSocket)ssf.createServerSocket()).getSupportedProtocols();
                for (String p : supported_secure_protocols) Log.w("httpd", "SSL supported protocol: "+p);
                m_micro_httpd.makeSecure(ssf, null); // new String[] { "TLSv1" , "TLSv1.1", "TLSv1.2" });
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
            m_micro_httpd.m_root_folders.put("tools", getExternalFilesDir(null));

            // Copy Resources to ExternalFiles
            //File file = new File(getExternalFilesDir(null), "index.html");
            //if (!file.exists())
            {
                HashMap<String, Integer> deps = new HashMap<>();
                deps.put("index.html", R.raw.index_html);
                deps.put("list_js.js", R.raw.list_js);
                deps.put("upload_js.js", R.raw.upload_js);
                deps.put("jquery_min.js", R.raw.jquery_min_js);
                deps.put("handsontable_full_min_js.js", R.raw.handsontable_full_min_js);
                deps.put("handsontable_full_min_css.css", R.raw.handsontable_full_min_css);


                for (Map.Entry<String, Integer> entry : deps.entrySet())
                {
                    InputStream is = getResources().openRawResource(entry.getValue());
                    OutputStream os = new FileOutputStream(new File(getExternalFilesDir(null), entry.getKey()));

                    int total_count = is.available();
                    byte[] data = new byte[total_count];
                    int read_count = is.read(data);
                    os.write(data);
                    is.close();
                    os.close();
                    if (read_count!=total_count)
                        Log.e("httpd", "failed to fully read and save "+entry.getKey());
                }
            }
        } catch (Exception e) {
            Log.e("httpd", "Resources2External error: " + e.toString());
        }

        // requires API 14+
        /*
        int api_version = android.os.Build.VERSION.SDK_INT;
        if (api_version>=14) {
            try {
                m_p2p_intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                m_p2p_intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                m_p2p_intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                m_p2p_intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

                m_p2p_manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                m_p2p_channel = m_p2p_manager.initialize(this, getMainLooper(), null);
            } catch (Exception e) {
                Log.e("httpd", "P2P error: " + e.toString());
            }
        } else {
            Log.w("httpd", "Running on API "+api_version+", need 14+ to get Wifi name.");
        }
        */
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
                    Log.w("httpd", "Network interface: "+intf.getDisplayName());
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        Log.w("httpd", "Network address: "+inetAddress.toString());
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipHostname = inetAddress.getHostAddress();
                            Log.w("httpd", "IP Hostname "+ipHostname);
                            byte b[] = inetAddress.getAddress();
                            // (my_byte & 0xff) converts signed my_byte into a unsigned int
                            if (b.length==4) {
                                Log.w("httpd", "IP "+inetAddress.toString());
                                ipAddress = ((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff);
                            } else {
                                Log.w("httpd", "IP v6 / MAC " + inetAddress.toString());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("httpd", "Network exception: "+ ex.toString());
            }
            if (ipAddress==0)
                ipAddress = 0x1000000 + 127; // little endian
            String httpd_addr = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            boolean http_secure = getPreferences(MODE_PRIVATE).getBoolean("http_secure", MicroHTTPD.http_secure_default);
            final String base_url = (http_secure?"https":"http") + "://" + httpd_addr + ":" + m_micro_httpd.getListeningPort() + "/";
            final String query = "?secret="+m_micro_httpd.m_http_password;
            final String url = base_url + query;

            TextView tv = (TextView) findViewById(R.id.http_url);
            tv.setText(url);

            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            int qr_size = Math.min(width,height)>700 ? 512 : 256;

            ImageView iv = (ImageView) findViewById(R.id.http_qr_url);
            Bitmap bmp = makeQRImage(url, qr_size);
            if (bmp!=null) iv.setImageBitmap(bmp);
            /*
            iv.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            });
            */
        }
        catch( Exception e )
        {
            System.err.println( "Couldn't get server IP:\n" + e );
            System.exit( -1 );
        }

        /*
        try {
            BroadcastReceiver p2p_broadcast_receiver = new WiFiDirectBroadcastReceiver();
            registerReceiver(p2p_broadcast_receiver, m_p2p_intentFilter);
        } catch ( Exception e ) {
            Log.w("httpd", "couldn't get Wifi PnP access to retrieve device name. "+e.getMessage());
        }
        */

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void sync_setting_checkbox(final String setting_name, int res_id, boolean default_value, final String msg_on_true, final String msg_on_false, final boolean restart_on_change) {
        if (!getPreferences(MODE_PRIVATE).contains(setting_name))
            getPreferences(MODE_PRIVATE).edit().putBoolean(setting_name, default_value).commit();

        Boolean b = getPreferences(MODE_PRIVATE).getBoolean(setting_name, default_value);

        final CheckBox ckbx = (CheckBox) findViewById( res_id );
        ckbx.setChecked(b);
        ckbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //CheckBox ckbx = (CheckBox) findViewById( res_id );

                SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
                ed.putBoolean(setting_name, isChecked);
                ed.commit();

                if (isChecked && msg_on_true != null)
                    Toast.makeText(getApplicationContext(), msg_on_true, Toast.LENGTH_LONG).show();
                if (!isChecked && msg_on_false != null)
                    Toast.makeText(getApplicationContext(), msg_on_false, Toast.LENGTH_LONG).show();
                if (restart_on_change)
                    restart_activity(getApplicationContext());
            }
        });

    }

    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static Random rnd;
    private static String random_password(int len) {
        if (rnd==null) rnd = new Random();
        StringBuilder sb = new StringBuilder( len );
        for (int i=0; i<len; i++)
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    private void restart_activity(Context context) {
        m_micro_httpd.stop();

        Intent mStartActivity = new Intent(context, MicroHTTPDActivity.class);
        int pendingIntentId = 0; //123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, pendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm_mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm_mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

        finish();
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

/*
class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w("httpd", "WiFiDirectBroadcastReceiver.onReceive action: " + action);

        int api_version = android.os.Build.VERSION.SDK_INT;
        if (api_version>=14) {
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.w("httpd_p2p", "***** DEVICE NAME " + device.deviceName + " *****");
            }
        }
    }
}
*/

/*
class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MicroHTTPDActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
*/

class MicroHTTPD extends NanoHTTPD {
    public SharedPreferences m_preferences = null;
    public String m_http_password;
    public Map<String, File> m_root_folders = new HashMap<>();

    public MicroHTTPD() throws IOException {
        super(8086); // 0 to select port automatically
        super.MIME_TYPES = this.MIME_TYPES;

        // lines below may throw
        //m_root_folders.put("documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
        m_root_folders.put("downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        m_root_folders.put("movies", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
        m_root_folders.put("music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        m_root_folders.put("photos", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        m_root_folders.put("pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        //m_root_folders.put("podcasts", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS));
        m_root_folders.put("root", new File("/"));
        m_root_folders.put("sdcard", Environment.getExternalStorageDirectory());
        //m_root_folders.put("tools",  getExternalFilesDir(null));

    }

    public File full_path(String uri) {
        String[] uri_split = uri.split("/");
        String uri_root_folder = uri_split.length>1 ? uri_split[1] : "";
        File f = null;
        if (m_root_folders.containsKey(uri_root_folder)) {
            f = new File(m_root_folders.get(uri_root_folder), uri.substring(uri_root_folder.length()+1));
        } else if (uri.equals("") || uri.equals("/")) {
            f = new File("/");
        } else {
            String msg = "full_path(uri='"+uri+"'): unknown root folder '"+uri_root_folder+"'.";
            Log.e("httpd", msg);
        }
        return f;
    }

    static public boolean
        http_secure_default = false,
        http_allow_put_default = true,
        http_allow_delete_default = false,
        http_auth_get_default = false,
        http_auth_put_default = true,
        http_auth_delete_default = true;

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Map<String, String> params = session.getParms(); // parsed query
        String query = session.getQueryParameterString(); // raw query
        String uri = session.getUri(); // excl query
        Method verb = session.getMethod(); // GET, POST, PUT, etc.

        String msg = "";

        boolean bAuthOk = false;
        if (headers.containsKey("authorization")) try {
            String auth64 = headers.get("authorization").replace("Basic ", "");
            Log.w("httpd", "Authorization base64: "+auth64);
            byte[] data = Base64.decode(auth64, Base64.DEFAULT);
            String auth = new String(data, "UTF-8");
            Log.w("httpd", "Authorization: "+auth);
            msg += "Authorization: request uses secret '"+auth+"'.";
            bAuthOk = auth.contains(m_http_password);
        } catch (UnsupportedEncodingException uee) {
            Log.e("httpd", "Authorization header error: "+uee);
        }

        Log.w("httpd", "HTTP "+verb.toString()+" "+uri);
        File f = full_path(uri);
        if (f==null) {
            // URI root folder was not listed in m_root_folders
            return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, ".");
        }

        if (verb == Method.OPTIONS) {
            // OPTIONS (to ask permission to GET, POST, PUT)
            return NanoWebDAV.webdav_options(session);
        } else if (verb == Method.PUT || verb == Method.POST) {
            // PUT
            if (!m_preferences.getBoolean("http_allow_put", http_allow_put_default))
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: http_allow_put set to false.");
            if (m_preferences.getBoolean("http_auth_put", http_auth_put_default) && !bAuthOk)
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: http_auth_put set to true and authentication failed.");

            try {
                //BufferedReader reader = new BufferedReader(new InputStreamReader(session.getInputStream()));
                //String line;
                //while ((line = reader.readLine()) != null)
                //    msg += line + "\n";
                //session.get

                Map<String, String> files = new HashMap<>();
                session.parseBody(files);

                Log.w("httpd", "Headers:");
                msg += "\n" + "Headers:" + "\n";
                for (String h : headers.keySet()) {
                    String tmp = "header['" + h + "'] : " + headers.get(h);
                    Log.w("httpd", tmp);
                    msg += tmp + "\n";
                }

                Log.w("httpd", "Query params:");
                msg += "\n" + "Query params:" + "\n";
                for (String pk : params.keySet()) {
                    String tmp = "prms['" + pk + "'] : " + params.get(pk)
                            + " \nprms['" + pk + "'] : " + new String(params.get(pk).getBytes("ISO-8859-1"), "UTF-8"); // US-ASCII, ISO-8859-1, UTF-8
                    Log.w("httpd", tmp);
                    msg += tmp + "\n";
                }

                for (String fk : files.keySet()) {

                    // create parent folder
                    if (!f.exists()) {
                        if (uri.endsWith("/"))
                            f.mkdirs();
                        else if (f.getParentFile() != null && !f.getParentFile().exists())
                            f.getParentFile().mkdirs();
                    }

                    if (f.isDirectory()) {
                        if (params.containsKey(fk))
                            f = new File(f.getCanonicalPath()+"/"+params.get(fk)).getAbsoluteFile();
                        else
                            throw new Exception("write destination is a directory and didn't get a file name");
                    }

                    File file_src = new File(files.get(fk));
                    File file_dst = new File(f.getCanonicalPath());

                    String tmp = "file['" + fk + "'] : " + file_src.getCanonicalPath() + " " + file_src.length() + "bytes -> " + file_dst.getCanonicalPath();
                    Log.w("httpd", tmp);
                    msg += tmp + "\n";

                    InputStream in = new FileInputStream(file_src);
                    OutputStream out = new FileOutputStream(file_dst);

                    int read;
                    byte[] buffer = new byte[1024];
                    while ((read = in.read(buffer)) != -1)
                        out.write(buffer, 0, read);

                    in.close();
                    out.flush();
                    out.close();

                    // use "Last-Modified" header if any
                    String last_modified_text = null;
                    if (headers.containsKey("last-modified"))
                        last_modified_text = headers.get("last-modified");
                    if (params.containsKey("last-modified"))
                        last_modified_text = params.get("last-modified");
                    if (params.containsKey("Last-Modified"))
                        last_modified_text = params.get("Last-Modified");
                    if (last_modified_text != null && !last_modified_text.isEmpty()) {
                        msg += "last_modified_text: " + last_modified_text + "\n";
                        SimpleDateFormat date_fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); // RFC-7231
                        if (last_modified_text.length()==10)
                            date_fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US); // ISO 8601
                        if (last_modified_text.length()==16)
                            date_fmt = new SimpleDateFormat("yyyy-MM-dd'"+last_modified_text.charAt(10)+"'HH:mm", Locale.US);
                        if (last_modified_text.length()==17)
                            date_fmt = new SimpleDateFormat("yyyy-MM-dd'"+last_modified_text.charAt(10)+"'HH:mm'"+last_modified_text.charAt(16)+"'", Locale.US);
                        Date last_modified_date = date_fmt.parse(last_modified_text);
                        long last_modified_time = last_modified_date.getTime(); // milliseconds since January 1, 1970, 00:00:00 GMT.
                        msg += "last_modified_time: " + last_modified_time + "\n";
                        boolean b = file_dst.setLastModified(last_modified_time);
                        if (!b) Log.e("httpd", "setLastModified("+file_dst.getAbsolutePath()+","+last_modified_date+") returned false");
                    }

                    msg += "Done "+file_src.getCanonicalPath() + " -> " + file_dst.getCanonicalPath() + "\n";
                }
                return createResponse(Response.Status.OK, "text/plain", "");
            } catch (Exception e) {
                msg += "error while writing file " + uri + "\n" + stack_trace(e) + "\n\n";
                Log.e("httpd", msg);
                return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", msg);
            }
        } else if (verb == Method.DELETE) {
            // DELETE
            if (!m_preferences.getBoolean("http_allow_delete", http_allow_delete_default))
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: http_allow_delete set to false.");
            if (m_preferences.getBoolean("http_auth_delete", http_auth_delete_default) && !bAuthOk)
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: http_auth_delete set to true and authentication failed.");

            if (f==null || !f.exists()) {
                return createResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "NOT_FOUND: "+uri+" not found.\n");
            } else {
                boolean bOk;
                if (f.isDirectory())
                    bOk = NanoWebDAV.recursive_delete(f);
                else
                    bOk = f.delete();

                if (bOk)
                    return createResponse(Response.Status.NO_CONTENT, null, null); // NanoHTTPD.MIME_PLAINTEXT, "OK: path "+uri+" has been deleted.\n");
                else
                    return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "ERROR: path "+uri+" has NOT been deleted.\n");
            }
        } else if (verb == Method.GET || verb == Method.HEAD) {
            if (m_preferences.getBoolean("http_auth_get", http_auth_get_default) && !bAuthOk)
                return createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: http_auth_get set to true and authentication failed.");

            if (uri.equals("/")) {
                if (params.containsKey("list")) {
                    // list directory
                    if (params.containsKey("list") && !params.get("list").equalsIgnoreCase("html"))
                        return listRootDirectoryJson();
                    else
                        return listRootDirectoryHtml();
                } else {
                    String redirect_url = "/tools/index.html" + (query!=null?"?"+query:"");
                    Response res = createResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML,
                        "<html><body>Redirected: <a href=\"" + redirect_url + "\">" + redirect_url + "</a></body></html>");
                    res.addHeader("Location", redirect_url);
                    return res;
                }
            } else if (f != null && f.exists()) {
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
                    return serveFile(f.getAbsolutePath(), headers, f, mimeTypeForFile);
                } else {
                    // folder
                    boolean index_html_exists = new File(f.getAbsolutePath() + "/index.html").exists();
                    if (!index_html_exists || params.containsKey("list")) {
                        // list directory
                        boolean calculate_hashes = false;
                        if (params.containsKey("hash"))
                            calculate_hashes = Boolean.parseBoolean(params.get("hash"));
                        if (params.containsKey("list") && !params.get("list").equalsIgnoreCase("html"))
                            return listDirectoryJson(uri, f, calculate_hashes);
                        else
                            return listDirectoryHtml(uri, f);
                    } else {
                        String mimeTypeForFile = getMimeTypeForFile(uri);
                        return serveFile(uri, headers, new File(f.getAbsolutePath() + "/index.html"), mimeTypeForFile);
                    }
                }
            } else {
                // GET but uri does not exist
                return createResponse(Response.Status.NOT_FOUND, "text/plain", uri + " not found\n");
            }
        }

        else {
            try {
                if (verb == Method.PROPFIND)
                    return NanoWebDAV.webdav_propfind(uri, f, session, this);
                else if (verb == Method.PROPPATCH)
                    return NanoWebDAV.webdav_proppatch(session);
                else if (verb == Method.MKCOL)
                    return NanoWebDAV.webdav_mkcol(uri, f, session);
                else if (verb == Method.MOVE)
                    return NanoWebDAV.webdav_move(uri, f, session, this);
                else if (verb == Method.COPY)
                    return NanoWebDAV.webdav_copy(uri, f, session, this);
                else if (verb == Method.DELETE)
                    return NanoWebDAV.webdav_delete(uri, f, session);
                else if (verb == Method.LOCK)
                    return NanoWebDAV.webdav_lock(session);
                else if (verb == Method.UNLOCK)
                    return NanoWebDAV.webdav_unlock(session);
                else
                    return createResponse(Response.Status.NOT_IMPLEMENTED, "text/plain", "HTTP verb "+verb.toString()+" not supported");
            } catch (Exception e) {
                return createResponse(Response.Status.INTERNAL_ERROR, "text/plain", "HTTP WEBDAV EXCEPTION "+e.getMessage());
            }
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

    protected Response listDirectoryJson(String uri, File f, boolean calculate_hashes) {

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

    protected Response listDirectoryHtml(String uri, File f) {

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

        StringBuilder sb = new StringBuilder();
        try {
            sb.append("<html><body>");
            for (String directory : directories) {
                sb.append(String.format("<a href=\"%s/\">%s/</a><br/>", directory, directory));
            }
            for (String file : files) {
                sb.append(String.format("<a href=\"%s\">%s</a><br/>", file, file));
            }
            sb.append("</body></html>");

            return createResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return createResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, e.getMessage());
        }
    }

    protected Response listRootDirectoryJson() {
        JSONArray ls = new JSONArray();
        for (String directory : m_root_folders.keySet()) {
            JSONObject item = new JSONObject();
            try { item.put("name", directory+"/"); } catch (Exception e) {}
            ls.put(item);
        }
        return createResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, ls.toString());
    }

    protected Response listRootDirectoryHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        for (String directory : m_root_folders.keySet())
            sb.append(String.format("<a href=\"%s/\">%s/</a><br/>", directory, directory));
        sb.append("</body></html>");
        return createResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, sb.toString());
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Response.IStatus status, String mimeType, InputStream message, int length) {
        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message, length);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, PROPFIND, PROPPATCH, MKCOL, MOVE, COPY, LOCK, UNLOCK");
        res.addHeader("Access-Control-Allow-Headers", "*");
        return res;
    }

    // Announce that the file server accepts partial content requests
    public Response createResponse(Response.Status status, String mimeType, String message) {
        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "HEAD, GET, PUT, POST, DELETE, OPTIONS, PROPFIND, PROPPATCH, MKCOL, MOVE, COPY, LOCK, UNLOCK");
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
    private static Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
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

    private static String file_md5(File f) {
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
            //byte mdBytes2[] = digest.digest();


            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : mdBytes) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();


        } catch (NoSuchAlgorithmException | IOException e) {
            Log.w("httpd", "md5 "+e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    private String stack_trace(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter( writer );
        e.printStackTrace(printWriter );
        printWriter.flush();

        return e.toString() + "\n" + writer.toString();
    }
}