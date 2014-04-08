/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servlets;

import facebook4j.internal.org.json.JSONArray;
import facebook4j.internal.org.json.JSONException;
import facebook4j.internal.org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; 
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

/**
 *
 * @author Ortin
 */
@WebServlet(name = "QRGen", urlPatterns = {"/QRGen"})
public class QRGen extends HttpServlet {
    
    //private final String QR_PATH = System.getProperty("user.dir").concat("/Huntsperson/");
    private final String APP_ID = "480920812033752";
    private final String APP_SECRET = "ff24167e6a9505d11c89a4b7bea5d0a8";
    private final String GRAPH_API = "https://graph.facebook.com";
    private final String ACCESS_TOKEN = APP_ID + "|" + APP_SECRET;
    private final String RAND_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int RAND_LEN = 32;
    private final int PNG_URL_INDEX = 0;
    private final int CLUE_INDEX = 1;

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("index.jsp");
        response.flushBuffer();
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String strAction = request.getParameter("action");
        
        if (strAction == null) {
            response.sendError(400, "Null request sent.");
            response.flushBuffer();
        }
        else if (strAction.equalsIgnoreCase("submit")) {
            this.processClues(request, response);
            response.setStatus(200);
        }
        else if (strAction.equalsIgnoreCase("verify")) {
            String clueid = request.getParameter("clueid");
            String cluecode = request.getParameter("cluecode");
            String userid = request.getParameter("userid");
            String accessToken = request.getParameter("accessToken");
            String cluepostid = this.verifyQR(clueid, cluecode);
            if (cluepostid.equals("")) {
                response.sendError(420, "The provided QR code failed to match any Huntsperson entry.");
                return;
            }
            if (postSuccess(cluepostid, userid, accessToken).equals("")) {
                response.sendError(420, "Failed to post the success to Facebook.");
                return;
            }
            response.setStatus(200);
        }
        else
        {
            response.sendError(400, "The action specified cannot be handled by the server.");
        }
    }

    /**
     * Processes the set of clues passed to the servlet by the webpage, and 
     * generates a QR code for each clue.  Stores the clues in a database.
     * 
     * Database has columns NUMERIC CLUEID, VARCHAR FBGROUPID, and VARCHAR CLUE
     * 
     * CLUEID is an eight digit number used to index the clue.  The first six digits 
     * represent the group the clue responds to.  The last two digits represent the clue 
     * number.
     * 
     * @param request The Http Request object originating the request
     * @param response The Http Response object directed to the requesting object
     * @throws java.io.IOException
     */
    protected void processClues(HttpServletRequest request, HttpServletResponse response) throws IOException
    {   
        String groupid = this.makeGroup(request);
        if (groupid.equalsIgnoreCase("")) { 
            this.endThread(response, 500, "Facebook API call failed: failed to create new group.");
        }

        int groupIncrementor = 100; //increments the group number of CLUEID by one
        
        int groupNum = this.getCurClueID(groupIncrementor);
        if (groupNum == 0) {
            this.endThread(response, 500, "Failed to acquire the next available group number.");
        }
        
        //Grabs form data for loops made later.
        String strNum = request.getParameter("numClues");
        int intNum = Integer.parseInt(strNum);
        
        ArrayList<ArrayList<String>> printData = genQRCode(groupNum, intNum, groupid, request);
                
        if (printData.isEmpty()) {
            this.endThread(response, 500, "Failed to generate the QR codes needed by Huntsperson.");
        } else {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            
            out = this.makePrintablePage(printData, groupid, out);
            
            out.close();
        }
    }
    // </editor-fold>
    
    /**
     * Performs an SQL call to check which CLUEID to begin generating the new 
     * clue set with.
     * 
     * @param incrementor The value to increase the multi part ClueID by per clue.
     * @return An integer corresponding to the current ClueID, or zero if the call failed
     */
    protected int getCurClueID(int incrementor) {
        
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        int num = 0;
        
        try {
            // <editor-fold defaultstate="collapsed" desc="Core logic for SQL call.">
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Huntsperson");
            
            con = ds.getConnection();
            st = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            
            //Grabs values
            rs = st.executeQuery("SELECT MAX(CLUEID) FROM CLUETABLE");
            
            Logger lgr = Logger.getLogger("processClues");
            if (!rs.first()) { lgr.log(Level.INFO, 
                    "SQL query SELECT MAX(CLUEID) FROM CLUETABLE returned null pointer.");
            }
            else {
                num = rs.getInt(1);
                lgr.log(Level.INFO, 
                    "SQL query SELECT MAX(CLUEID) FROM CLUETABLE returned {0}", num);
            }
            num = num + incrementor;
            // </editor-fold>
        } catch (SQLException e){
            this.logError(e, "getCurClueID");
        } catch (NamingException e){
            this.logError(e, "getCurClueID");
        } finally {
            //I have no idea what should go here, if anything.
        }
        
        return num;
    }
    
    /**
     * Makes a Facebook group using a POST request and returns the group id 
     * of the new group.
     * 
     * @param request The request object that contains the parameters used to produce
     * the Facebook Group
     * @return The group id of the Facebook group, or an empty string if the Facebook
     * API call fails.
     */
    protected String makeGroup(HttpServletRequest request) {
        String groupid = "";
      
        URL                 url;
        URLConnection       urlConn;
        DataOutputStream    printout;
        DataInputStream     input;
        
        String groupName = request.getParameter("groupName");
        String groupDesc = request.getParameter("groupDesc");
        String groupAdmin = request.getParameter("userID");
 
        try {
            // URL of Facebook Graph API
            url = new URL (GRAPH_API + "/" + APP_ID + "/groups");
            // URL connection channel.
            urlConn = url.openConnection();
            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput (true);
            // Let the RTS know that we want to do output.
            urlConn.setDoOutput (true);
            // No caching, we want the real thing.
            urlConn.setUseCaches (false);
            // Specify the content type.
            urlConn.setRequestProperty
            ("Content-Type", "application/x-www-form-urlencoded");
            // Send POST output.
        
            printout = new DataOutputStream (urlConn.getOutputStream ());

            String content = "name=" + URLEncoder.encode(groupName, "UTF-8") 
                    + "&description=" + URLEncoder.encode(groupDesc, "UTF-8") 
                    + "&admin=" + URLEncoder.encode(groupAdmin, "UTF-8") 
                    + "&access_token=" + ACCESS_TOKEN;
            
            this.logInfo("Facebook publish group request sent with parameters "
                    + "Name=" + URLEncoder.encode(groupName, "UTF-8")
                    + ", Description=" + URLEncoder.encode(groupDesc, "UTF-8")
                    + ", Admin=" + URLEncoder.encode(groupAdmin, "UTF-8") , "makeGroup");
            
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            // Get response data.
            input = new DataInputStream (urlConn.getInputStream ());
            
            JSONObject response = new JSONObject(input.readLine());
            groupid = response.getString("id");
            this.logInfo(response.toString(), "makeGroup");
            this.logInfo(groupid, "makeGroup");

            input.close();
            } catch (IOException e) {
                this.logError(e, "makeGroup");
            } catch (JSONException e) {
                this.logError(e, "makeGroup");
            }
        
        return groupid;
    }
    
    /**
     * Makes a post on the specified group's wall containing the specified clue.
     * 
     * @param groupid The id of the group to post to.
     * @param clue The clue to be posted.
     * @param num The clue number we are posting.
     * @param userToken An access token with permissions to post on the group.
     * @return The post ID of the post containing the clue, or an empty string on failure.
     */
    protected String postToGroup(String groupid, String clue, int num, String userToken) {
        String postid = "";
        
        URL                 url;
        HttpURLConnection   urlConn;
        DataOutputStream    printout;
        DataInputStream     input;
        
        try {
            // URL of Facebook Graph API
            url = new URL (GRAPH_API + "/" + groupid + "/feed");
            // URL connection channel.
            urlConn = (HttpURLConnection) url.openConnection();
            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput (true);
            // Let the RTS know that we want to do output.
            urlConn.setDoOutput (true);
            // No caching, we want the real thing.
            urlConn.setUseCaches (false);
            // Specify the content type.
            urlConn.setRequestProperty
            ("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestMethod("POST");
            // Send POST output.
        
            printout = new DataOutputStream (urlConn.getOutputStream ());

            String content = "message=Clue+" + String.valueOf(num + 1) + ":+" + URLEncoder.encode(clue, "UTF-8")
                    + "&access_token=" + userToken;
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            // Get response data.
            input = new DataInputStream (urlConn.getInputStream ());
            
            JSONObject response = new JSONObject(input.readLine());
            postid = response.getString("id");
            this.logInfo(response.toString(), "postToGroup");
            this.logInfo(postid, "postToGroup");

            input.close();
            } catch (IOException e) {
                this.logError(e, "postToGroup");
            } catch (JSONException e) {
                this.logError(e, "postToGroup");
            }
        
        return postid;
    }
    
    /**
     * A helper method that sets up the architecture to generate the QR codes needed
     * for a new Huntsperson group.
     * 
     * Invokes parseClueID(int), genRandString (Random, String, int), genQRData(int, String,),
     * makeQR(String, int), postToGroup(String, String), and insertClueIntoTable(HttpServletRequest, String, int, int)
     * 
     * @param groupNum The base group number.
     * @param intNum The number of clues to generate.
     * @param groupid The group_id of the facebook group for this Huntsperson group.
     * @param request The request object originating this request.
     * @return A non-empty ArrayList if the codes were generated successfully, an empty
     * ArrayList on failure.
     */
    protected ArrayList genQRCode(int groupNum, int intNum, String groupid, HttpServletRequest request) {
        
        groupNum = this.parseClueID(groupNum);
       
        /* The localPath is the path on disk leading to the servlet context path.
         * The rootpath is the relative path to the resource on the servlet.
         * The absolute path is the combination of these two paths.
         *
         * The local path is used to generate a File object to save QR Codes to.  It
         * does this in conjunction with the rootpath.
         * The rootpath is used to generate a URL that can be displayed in an HTML form
         */
        String localPath = this.getServletConfig().getServletContext().getRealPath("/");
        String rootpath = "QRGen/" + String.valueOf(groupNum) + "/";
        String absolutePath = localPath + rootpath;
        ArrayList<ArrayList<String>> printData = new ArrayList<ArrayList<String>>(intNum);
        
        //First time run logic, checks whether the QR directory exists.
        if (!new File(absolutePath).exists()) { new File(absolutePath).mkdirs(); }
        
        for (int i = 0; i < intNum; i++)
        {
            //Code for generating QR codes for each clue goes here.
            groupNum++;
            String cluecode = this.genRandString(new Random(), RAND_CHARS, RAND_LEN);
            String qrData = this.genQRData(groupNum, cluecode);
            //Failure case
            if (qrData.equals("")) { return new ArrayList(); }
            
            try { 
                String filepath = absolutePath.concat(String.valueOf(groupNum).concat(".png"));
                //The act of checking this conditional creates the QR Code.  If
                //the code is not made then false is returned.
                if (!this.makeQR(qrData, groupNum, filepath)) {return new ArrayList();}
                
            } catch (IOException e) {
                this.logError(e, "makeQR");
                return new ArrayList();
            } 
            String clue = request.getParameter(("clue").concat(String.valueOf(i)));
            
            String userToken = request.getParameter("userToken");
            String postid = this.postToGroup(groupid, clue, i, userToken);
            if (postid.equals("")) { return new ArrayList(); }
            
            ArrayList<String> data = new ArrayList(2);
            data.add(PNG_URL_INDEX, rootpath.concat(String.valueOf(groupNum).concat(".png")));
            data.add(CLUE_INDEX, clue);
            printData.add(data);
            
            this.insertClueIntoTable(request, cluecode, groupid, groupNum, clue, postid);
        }
        
        return printData;
    }
    
    /**
     * Generates a QR code encoded with the specified data, and saved to the specified
     * filepath with a file name derived from groupNum
     * 
     * @param qrData The data to insert into the QR code.
     * @param groupNum A unique number associated with a QRCode in the file system.
     * @param filepath The path in the filesystem to create the code in.
     * @return
     * @throws IOException 
     */
    protected boolean makeQR(String qrData, int groupNum, String filepath) throws IOException {
        FileOutputStream fout = null;
        
        try {
            ByteArrayOutputStream out = QRCode.from(qrData).to(ImageType.PNG).stream();
            
            fout = new FileOutputStream(new File(filepath));
            fout.write(out.toByteArray());
            fout.flush();
                
            Logger lgr = Logger.getLogger("makeQR");
            lgr.log(Level.INFO, "Created QRCode at {0}.", filepath);
            
        } catch (FileNotFoundException e) {
            this.logError(e, "makeQR");
            return false;
                
        } catch (IOException e) {
            this.logError(e, "makeQR");
            return false;
            
        } finally {
            if (fout != null) { fout.close(); }
        }
        return true;
    }
    
    /**
     * Inserts a clue passed in via HttpRequest into the database, associated with
     * a clue and a facebook group id.
     * 
     * @param request The HttpServletRequest object that originated this call.  
     * Contains the CLUE string in form data in "clue" + i format.
     * @param clueCode A unique code used to ensure QRCodes are only scanned from valid sources
     * @param fbid The facebook group id that this clue is associated with.
     * @param groupNum The CLUEID of the given clue.
     * @param clue The description of the given clue.
     * @param postid The id of the post this clue is contained in on the group wall.
     */
    protected void insertClueIntoTable(
            HttpServletRequest request, String clueCode, String fbid, int groupNum, String clue, String postid) {
        try {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
                
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Huntsperson");
                
            con = ds.getConnection();
            st = con.createStatement();
                
            String insert = "INSERT INTO CLUETABLE (CLUEID,FBGROUPID,CLUE,CLUECODE,CLUEPOSTID) ";
            String values = "VALUES (" + groupNum + ",'" + fbid + "','" + clue + "','" + clueCode 
                    + "','" + postid + "')";
               
            int rows = st.executeUpdate(insert + values);
            
            Logger lgr = Logger.getLogger("insertClueIntoTable");
            lgr.log(Level.INFO, "Executed SQL statement {0}{1}", new Object[]{insert, values});
            
        } catch (SQLException e){
            this.logError(e, "insertClueIntoTable");
        } catch (NamingException e){
            this.logError(e, "insertClueIntoTable");
        } finally {
        //I have no idea what should go here, if anything.
        }
    }
    
    /**
     * Cleans up the integer returned by getCurClueID so it can be used by the
     * QRGen logic.  Cleaning up means reverting the last two digits of groupNum to
     * zeroes.
     * 
     * @param intNum The integer to parse.
     * @return A cleaned integer used to begin the cluemaking logic.
     */
    int parseClueID(int groupNum)
    {
        int catInt;
        String groupStr = String.valueOf(groupNum);
        int strlen = groupStr.length();
        String catStr = groupStr.substring(0, strlen - 2);
        groupStr = catStr.concat("00");
        catInt = Integer.valueOf(groupStr);
        
        Logger lgr = Logger.getLogger("parseClueID");
        lgr.log(Level.INFO, "parseClueID returned value of {0} from {1}", new Object[]{catInt, groupNum});
        
        return catInt;
    }
    
    /**
     * Generates a random string of the specified length chosen from the specified characters
     * @param rng A series of randomly generated numbers
     * @param characters The characters to select the content of the random string from
     * @param length The length of the string
     * @return A string of randomly generated characters.  Multiple invocations of this method
     * are not guaranteed to produce unique values.
     */
    protected String genRandString(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        this.logInfo("Random string '" + text + "' was created.", "genRandString");
        return new String(text);
    }
    
    /**
     * Generates a string representation of a JSON object that can be used as QR Data to verify
     * that a given request to the server represents a user locating the QR code in real life.
     * 
     * The JSON object has fields "clueid" and "cluecode", and uses as data the string 
     * representation of groupNum and a pseudorandomly generated number.
     * 
     * @param clueid
     * @param cluecode
     * 
     * @return A string representation of a JSON object with the appropriate fields, or an empty
     * string if the method fails.
     */
    protected String genQRData(int clueid, String cluecode) {
        
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();
        JSONObject mainJo = new JSONObject();
        String jsonStr = "";
        try {
            jo.put("clueid", String.valueOf(clueid));
            jo.put("cluecode", cluecode);
            ja.put(jo);
            mainJo.put("data", ja);
        } catch (JSONException e) {
            this.logError(e, "genQRData");
            return jsonStr;
        }
        jsonStr = mainJo.toString();
        this.logInfo("JSON array with contents " + jsonStr + " was created.", "genQRData");
        return jsonStr;
    }
    
    /**
     * Creates a printable form containing the QR codes generated by the application
     * as well as a permanent link to the facebook group containing the clues.
     * 
     * The permanent link is of the form:
     * http://facebook.com/pages/-/[group_id]?sk=app_[app_id]
     * @param data The clues and QR urls to format on the screen
     * @param groupid The Facebook group id of the associated Huntsperson group
     * @param out An empty printwriter to be flushed by the main servlet thread
     * @return A PrintWriter containing the bytes to flush to the user
     */
    protected PrintWriter makePrintablePage(
            ArrayList<ArrayList<String>> data, String groupid, PrintWriter out) {
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Printable Huntsperson Form</title>");
        out.println("</head>");
        out.println("<body>");
            out.println("<table>");
            for (int i = 0; i < data.size(); i++) {
                out.println("<tr>");
                    out.println("<td>");
                        out.println("<img src=\"" + data.get(i).get(PNG_URL_INDEX) 
                                + "\" "+ "alt=\"Clue " + String.valueOf(i) + "\">");
                    out.println("</td>");
                    out.println("<td>");
                        out.println("Clue " + (i+1) + ": " + data.get(i).get(CLUE_INDEX));
                    out.println("</td>");
            }
            out.println("</tr>");
            out.println("</table>");
            
            out.println("<a href=http://facebook.com/pages/-/" 
                    + groupid + "?sk=app_" 
                    + String.valueOf(APP_ID) + ">Permenant link to your group.</a>");
        out.println("</body>");
        out.println("</html>");
        
        return out;
    }
    
    /**
     * Posts a success message to the clue comment on behalf of the user.
     * @param cluepostid
     * @param userid
     * @param accessToken 
     */
    private String postSuccess(String cluepostid, String userid, String accessToken) {
        
        URL                 url;
        URLConnection       urlConn;
        DataOutputStream    printout;
        DataInputStream     input;
        
        String replyid = "";
 
        try {
            // URL of Facebook Graph API
            url = new URL (GRAPH_API + "/" + cluepostid + "/comments");
            // URL connection channel.
            urlConn = url.openConnection();
            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput (true);
            // Let the RTS know that we want to do output.
            urlConn.setDoOutput (true);
            // No caching, we want the real thing.
            urlConn.setUseCaches (false);
            // Specify the content type.
            urlConn.setRequestProperty
            ("Content-Type", "application/x-www-form-urlencoded");
            // Send POST output.
        
            printout = new DataOutputStream (urlConn.getOutputStream ());

            String content = "message=" + URLEncoder.encode("I found this clue!", "UTF-8") 
                    + "&access_token=" + URLEncoder.encode(accessToken, "UTF-8");
            
            this.logInfo("POSTed to " + url.toString() + "with message I found this clue!", "postSuccess");
            
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            // Get response data.
            input = new DataInputStream (urlConn.getInputStream ());
            
            JSONObject response = new JSONObject(input.readLine());
            replyid = response.getString("id");
            this.logInfo(response.toString(), "postSuccess");
            this.logInfo(replyid, "postSuccess");

            input.close();
            } catch (IOException e) {
                this.logError(e, "postSuccess");
            } catch (JSONException e) {
                this.logError(e, "postSuccess");
            }
        
        return replyid;
    }

    /**
     * Verifies that the clue id/code pair is a valid entry in the database.
     * 
     * @param clueid A unique clue id in numeric string format
     * @param cluecode A randomly generated string that is paired with the clue id
     * @return The postid of the clue that is to be solved, or a blank string if 
     * either of the parameters is incorrect.
     */
    private String verifyQR(String clueid, String cluecode) {
        
        String cluepostid = "";
        
        try {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
                
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Huntsperson");
                
            con = ds.getConnection();
            st = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                
            String insert = "SELECT CLUEPOSTID FROM CLUETABLE ";
            String values = "WHERE (CLUEID=" + Integer.valueOf(clueid) + " AND CLUECODE='" + cluecode + "')";
               
            rs = st.executeQuery(insert + values);
            //rs is expected to have only one entry, so 
            if (!rs.first()) { 
                this.logInfo(insert, values);
            }
            else {
                cluepostid = rs.getString(1);
                this.logInfo(insert, values);
            }
            
            Logger lgr = Logger.getLogger("verifyQR");
            lgr.log(Level.INFO, "Executed SQL statement {0}{1}", new Object[]{insert, values});
            
        } catch (SQLException e){
            this.logError(e, "verifyQR");
        } catch (NamingException e){
            this.logError(e, "verifyQR");
        } finally {
        //I have no idea what should go here, if anything.
        }
        
        return cluepostid;
    }
    
    /**
     * Logs an info level log.
     * 
     * @param info The info to be logged.
     * @param str The method the log was created from.
     */
    protected void logInfo(String info, String str)
    {
        Logger lgr = Logger.getLogger(str);
        lgr.log(Level.INFO, info);
    }
    
    /**
     * Logs an exception.
     * 
     * @param e The exception thrown by the program
     * @param str The method the exception was thrown in.
     */
    protected void logError(Exception e, String str)
    {
        Logger lgr = Logger.getLogger(str);
        lgr.log(Level.SEVERE, e.getMessage(), e);
    }
    
    /**
     * Terminates the thread and sends the specified error code to the invoking
     * webpage.
     * @param response The response for the originating web page.
     * @param returnCode The return code to send to the web page.
     * @param msg A message to be sent to the web page.
     * @throws java.io.IOException
     */
    protected void endThread(HttpServletResponse response, int returnCode, String msg) throws IOException {
        response.sendError(returnCode, msg);
        response.flushBuffer();
    }
    
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
// </editor-fold>
}
