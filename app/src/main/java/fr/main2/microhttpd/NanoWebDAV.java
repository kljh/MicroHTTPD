package fr.main2.microhttpd;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import fi.iki.elonen.NanoHTTPD;


public class NanoWebDAV {
    static final String no_content_mime = "application/xml; charset=utf-8"; // "application/xml; charset=utf-8"

    static void webdav_log_request(NanoHTTPD.IHTTPSession req) {
        Log.w("httpd_webdav", "request headers:\n" + req.getHeaders().toString() + "\n");
        //Log.d("httpd_webdav", "request body #" + req.body_text.length + ":\n" + req.body_text + "\n");
    }

    static NanoHTTPD.Response createDavResponse(NanoHTTPD.Response.Status status, String mimeType, String message) {
        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "HEAD,GET,POST,PUT,DELETE,OPTIONS,PROPFIND,PROPPATCH,MKCOL,MOVE,COPY,LOCK,UNLOCK");
        res.addHeader("Access-Control-Allow-Headers", "*");
        return res;
    }

    public static NanoHTTPD.Response webdav_options(NanoHTTPD.IHTTPSession req) {
        webdav_log_request(req);

        NanoHTTPD.Response res = createDavResponse(NanoHTTPD.Response.Status.OK, no_content_mime, "");
        res.addHeader("Allow", "HEAD,GET,POST,PUT,DELETE,OPTIONS,PROPFIND,PROPPATCH,MKCOL,MOVE,COPY,LOCK,UNLOCK");
        res.addHeader("MS-Author-Via", "DAV"); // mandatory fo WinXP
        res.addHeader("DAV", "1,2");
        res.addHeader("Content-Length", "0");

        Log.w("httpd_webdav", "options exit");

        return res;
    }

    public static NanoHTTPD.Response webdav_mkcol(String url_path, File full_path, NanoHTTPD.IHTTPSession req) {

        boolean bExist = full_path.exists();

        if (bExist)
            return createDavResponse(NanoHTTPD.Response.Status.FORBIDDEN, no_content_mime, "");

        try {
            full_path.mkdirs();
            return createDavResponse(NanoHTTPD.Response.Status.CREATED, no_content_mime, "");

        } catch (Exception e) {
            Log.e("httpd_webdav", "EXCEPTION MKCOL " + e.getMessage());
            return createDavResponse(NanoHTTPD.Response.Status.CONFLICT, no_content_mime, "");
        }
    }


    public static NanoHTTPD.Response webdav_move(String src_url_path, File src_full_path, NanoHTTPD.IHTTPSession req, MicroHTTPD httpd) {
        try {
            Map<String,String> headers = req.getHeaders();
            String dst_url_path = java.net.URLDecoder.decode(headers.get("destination"), "UTF-8");
            Log.d("httpd_webdav", "HTTP MOVE dst: " + dst_url_path);
            if (dst_url_path.startsWith("http")) {
                String dst_url_split[] = dst_url_path.split("/");
                Arrays.copyOfRange(dst_url_split, 3, dst_url_split.length);
                dst_url_path = TextUtils.join("/", dst_url_split);
            }

            File dst_full_path = httpd.full_path(dst_url_path);
            Log.d("httpd_webdav", "HTTP MOVE dst: " + dst_full_path);


            src_full_path.renameTo(dst_full_path);

            return createDavResponse(NanoHTTPD.Response.Status.CREATED, no_content_mime, "");
        } catch (Exception e) {
            Log.e("httpd_webdav", "EXCEPTION MOVE " + e.getMessage());
            return createDavResponse(NanoHTTPD.Response.Status.CONFLICT, no_content_mime, "");
        }
    }


    public static NanoHTTPD.Response webdav_copy(String src_url_path, File src_full_path, NanoHTTPD.IHTTPSession req, MicroHTTPD httpd) {
        try {
            Map<String,String> headers = req.getHeaders();
            String dst_url_path = java.net.URLDecoder.decode(headers.get("destination"), "UTF-8");
            Log.d("httpd_webdav", "HTTP COPY dst: " + dst_url_path);
            if (dst_url_path.startsWith("http")) {
                String dst_url_split[] = dst_url_path.split("/");
                Arrays.copyOfRange(dst_url_split, 3, dst_url_split.length);
                dst_url_path = TextUtils.join("/", dst_url_split);
            }

            File dst_full_path = httpd.full_path(dst_url_path);
            Log.d("httpd_webdav", "HTTP COPY dst: " + dst_full_path);

            throw new Exception("not implemeted / apparently not used by main WebDAV clients");
            //java.nio.file.StandardCopyOption.Files.copy(src_full_path, dst_full_path, COPY_ATTRIBUTES);

            //return createDavResponse(NanoHTTPD.Response.Status.CREATED, no_content_mime, "");
        } catch (Exception e) {
            Log.e("httpd_webdav", "EXCEPTION COPY " + e.getMessage());
            return createDavResponse(NanoHTTPD.Response.Status.CONFLICT, no_content_mime, "");
        }
    }



    public static NanoHTTPD.Response webdav_delete(String url_path, File full_path, NanoHTTPD.IHTTPSession req) {

        boolean bExist = full_path.exists();
        if (!bExist) {
            Log.w("httpd_webdav", "NOT FOUND " + full_path);
            return createDavResponse(NanoHTTPD.Response.Status.NOT_FOUND, no_content_mime, "");
        }


        try {
            if (full_path.isDirectory())
                recursive_delete(full_path);
            else
                full_path.delete();

            return createDavResponse(NanoHTTPD.Response.Status.NO_CONTENT, no_content_mime, "");
        } catch (Exception e) {
            Log.e("httpd_webdav", "EXCEPTION COPY " + e.getMessage());
            return createDavResponse(NanoHTTPD.Response.Status.CONFLICT, no_content_mime, "");
        }
    }

    static boolean recursive_delete(File folder_path) {
        File[] files = folder_path.listFiles();
        for(int i=0; i<files.length; i++) {
            if(files[i].isDirectory())
                recursive_delete(files[i]);
            else
                files[i].delete();
        }
        return folder_path.delete();
    }

    public static NanoHTTPD.Response webdav_propfind(String url_path, File full_path, NanoHTTPD.IHTTPSession req, MicroHTTPD httpd) throws Exception {
        Log.w("httpd_webdav", "propfind enters");

        String xml_header =
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                        "<d:multistatus" +
                        " xmlns:cs=\"http://calendarserver.org/ns/\"" +
                        " xmlns:cal=\"urn:ietf:params:xml:ns:caldav\"" +
                        " xmlns:card=\"urn:ietf:params:xml:ns:carddav\"" +
                        " xmlns:d=\"DAV:\">\n";
        String xml_footer =
                "</d:multistatus>";

        Map<String, String> headers = req.getHeaders();
        int depth = 1;
        if (headers.containsKey("depth")) {
            String depth_txt = headers.get("depth");
            if (depth_txt.length()>1)
                depth = 2; // "Infinity"
            else
                depth = Integer.parseInt(depth_txt); // "1" or "2"
        };
        Log.w("httpd_webdav", "depth: " + depth);

        if (depth==0) {

            if (url_path.equals("/") || (full_path!=null && full_path.exists())) {
                Log.w("httpd_webdav", "reply_body for zero depth");

                StringBuilder reply_body = new StringBuilder();
                reply_body.append(xml_header);
                webdav_propfind_response(reply_body, url_path, full_path, null);
                reply_body.append(xml_footer);

                NanoHTTPD.Response res = createDavResponse(NanoHTTPD.Response.Status.MULTI_STATUS, "application/xml; charset=utf-8", reply_body.toString());
                res.addHeader("DAV", "1,2");
                return res;

            } else {
                Log.w("httpd_webdav", "UNKNOWN PATH !! " + url_path);
                return createDavResponse(NanoHTTPD.Response.Status.NOT_FOUND, no_content_mime, "");
            }

        } else {
            // depth > 0

            Set<String> requested_fields = new HashSet<>();

            StringBuilder reply_body = new StringBuilder();
            reply_body.append(xml_header);

            // list also the root of the folder
            webdav_propfind_response(reply_body, url_path, full_path, requested_fields);

            File[] files =  (full_path!=null) ? full_path.listFiles() : null;
            if (files!=null) {
                for (File file: files) {
                    webdav_propfind_response(reply_body, (new File(url_path, file.getName())).getCanonicalPath(), file, requested_fields);
                }
            } else if (url_path.equals("/")) {
                for (String directory : httpd.m_root_folders.keySet()) {
                    webdav_propfind_response(reply_body, "/"+directory, httpd.m_root_folders.get(directory), requested_fields);
                }
            }

            reply_body.append(xml_footer);

            NanoHTTPD.Response res = createDavResponse(NanoHTTPD.Response.Status.MULTI_STATUS, "application/xml; charset=utf-8", reply_body.toString());
            res.addHeader("DAV", "1,2");
            return res;
        }

    }

    public static void webdav_propfind_response(StringBuilder response, String url_path, File full_path, Set<String> requested_fields) {
        Log.w("httpd_webdav", "webdav_propfind_response url_path: " + url_path);
        Log.w("httpd_webdav", "webdav_propfind_response full_path: " + full_path);


        boolean coll = full_path!=null && full_path.isDirectory();
        long size = full_path!=null && full_path.isFile() ? full_path.length() : 0;
        String last = full_path!=null && full_path.exists() ? toUTCString(full_path.lastModified()) : null; // Sat, 26 Dec 2015 15:38:20 GMT+00:00
        String crea = full_path!=null && full_path.exists() ? toISOString(full_path.lastModified()) : null;

        Log.w("httpd_webdav", "webdav_propfind_response coll: " + coll);

        if (full_path==null) {
            coll = true;
            last = "Sat, 26 Dec 2015 15:38:20 GMT+00:00";
            crea = "2015-12-25T00:00:00Z";
        }

        Log.w("httpd_webdav", "webdav_propfind_response coll: " + coll);

        String href = url_path + ( coll && !url_path.endsWith("/") ? "/" : "" );
        Log.w("httpd_webdav", "webdav_propfind_response href: " + href);

        String name = (new File(url_path)).getName();
        Log.w("httpd_webdav", "webdav_propfind_response name: " + name);
        if (name==null || name.equals("")) name = "root";
        Log.w("httpd_webdav", "webdav_propfind_response name: " + name);


        // skip invisible files
        if ((full_path!=null && full_path.isHidden()) || name.startsWith("."))
            return;

        Log.w("httpd_webdav", "webdav_propfind_response starting... ");

        response.append(
                "\t<d:response>\n" +
                "\t\t<d:href>" + href + "</d:href>\n" +
                "\t\t<d:propstat>\n" +
                "\t\t\t<d:prop>\n");

        if (coll) {
            response.append(
                "\t\t\t\t<d:creationdate/>\n" +                                    // ISO 8601
                "\t\t\t\t<d:displayname>"+name+"</d:displayname>\n" +              // Sounds ou temp-1687654484.tmp
                "\t\t\t\t<d:name>"+name+"</d:name>\n" +                            // Sounds ou temp-1687654484.tmp
                "\t\t\t\t<d:getcontentlength/>\n" +
                "\t\t\t\t<d:getcontenttype>text/html</d:getcontenttype>\n" +
                "\t\t\t\t<d:getlastmodified>"+last+"</d:getlastmodified>\n" +      // RFC 1123
                "\t\t\t\t<d:resourcetype><d:collection/></d:resourcetype>\n" +     // collection = folder
                // NOT IN RFC 4918 :
                "\t\t\t\t<d:iscollection>TRUE</d:iscollection>\n" +                // collection = folder
                "\t\t\t\t<d:isreadonly>TRUE</d:isreadonly>\n" +
                //"\t\t\t\t<d:href>http://192.168.43.163:8080"+href+"</d:href>\n" +  // http://192.168.43.163:8080/Sounds/
                "");
        } else {
            String mime = MicroHTTPD.getMimeTypeForFile(name);
            response.append(
                "\t\t\t\t<d:creationdate>"+crea+"</d:creationdate>\n" +
                "\t\t\t\t<d:displayname>"+name+"</d:displayname>\n" +
                "\t\t\t\t<d:name>"+name+"</d:name>\n" +
                "\t\t\t\t<d:getcontentlength>"+size+"</d:getcontentlength>\n" +
                "\t\t\t\t<d:getcontenttype>"+mime+"</d:getcontenttype>\n" +
                "\t\t\t\t<d:getlastmodified>"+last+"</d:getlastmodified>\n" +
                "\t\t\t\t<d:resourcetype/>\n" +
                // NOT IN RFC 4918 :
                "\t\t\t\t<d:iscollection>FALSE</d:iscollection>\n" +
                "\t\t\t\t<d:isreadonly>TRUE</d:isreadonly>\n" +
                //"\t\t\t\t<d:href>http://192.168.43.163:8080"+href+"</d:href>\n" +
                "");
        }

        response.append(
            "\t\t\t</d:prop>\n" +
            "\t\t\t<d:status>HTTP/1.1 200 OK</d:status>\n" +
            "\t\t</d:propstat>\n");

        if (requested_fields!=null) response.append(
            "\t\t<d:propstat>\n" +
            "\t\t\t<d:prop>\n" +
            "\t\t\t\t<d:isroot/>\n" +
            "\t\t\t\t<d:lastaccessed/>\n" +
            "\t\t\t\t<d:isstructureddocument/>\n" +
            "\t\t\t\t<d:parentname/>\n" +
            "\t\t\t\t<d:defaultdocument/>\n" +
            "\t\t\t\t<d:ishidden/>\n" +
            "\t\t\t\t<d:contentclass/>\n" +
            "\t\t\t\t<d:getcontentlanguage/>\n" +
            "\t\t\t</d:prop>\n" +
            "\t\t\t<d:status>HTTP/1.1 404 Not Found</d:status>\n" +
            "\t\t</d:propstat>\n");

        response.append(
                "\t</d:response>\n");
    }

    public static String toUTCString(long t) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(tz);
        return df.format(new Date(t));
    }

    public static String toISOString(long t) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date(t));
    }


    public static NanoHTTPD.Response webdav_proppatch(NanoHTTPD.IHTTPSession req) {
        webdav_log_request(req);

        String reply_body = "";
        return createDavResponse(NanoHTTPD.Response.Status.OK, "application/xml; charset=utf-8", reply_body);
    }

    public static NanoHTTPD.Response webdav_lock(NanoHTTPD.IHTTPSession req) {
        String reply_body =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<D:prop xmlns:D=\"DAV:\">\n" +
            "  <D:lockdiscovery>\n" +
            "    <D:activelock>\n" +
            "      <D:locktoken>\n" +
            "        <D:href>urn:uuid:e71d4fae-5dec-22d6-fea5-"+(new Date()).getTime()+"</D:href>\n" +
            "      </D:locktoken>\n" +
            "    </D:activelock>\n" +
            "  </D:lockdiscovery>\n" +
            "</D:prop>\n";

        return createDavResponse(NanoHTTPD.Response.Status.OK, "application/xml; charset=utf-8", reply_body);
    }

    public static NanoHTTPD.Response webdav_unlock(NanoHTTPD.IHTTPSession req) {
        return createDavResponse(NanoHTTPD.Response.Status.NO_CONTENT, no_content_mime, "");
    }
}