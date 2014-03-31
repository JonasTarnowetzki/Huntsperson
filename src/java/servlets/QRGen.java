/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servlets;

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
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    
    private final String QR_PATH = System.getProperty("user.dir").concat("/Huntsperson/QRCodes/");
    private final String APP_ID = "480920812033752";
    private final String APP_SECRET = "ff24167e6a9505d11c89a4b7bea5d0a8";
    private final String GRAPH_API = "https://graph.facebook.com";
    private final String ACCESS_TOKEN = APP_ID + "|" + APP_SECRET;

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
        response.sendError(401, "GET not implemented on servlet /QRGen");
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
        
        if (strAction == null)
        {
            response.sendError(400, "Null request sent.");
            response.flushBuffer();
        }
        else if (strAction.equalsIgnoreCase("submit"))
        {
            this.processClues(request, response);
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
     */
    protected void processClues(HttpServletRequest request, HttpServletResponse response) 
    {
        String fbid = this.makeGroup(request);
        
        int groupNum; 
        int groupIncrementor = 100; //increments the group number of CLUEID by one
        
        groupNum = this.getCurClueID(groupIncrementor);
        
        //Grabs form data for loops made later.
        String strNum = request.getParameter("numClues");
        int intNum = Integer.parseInt(strNum);
        
        this.genQRCode(groupNum, intNum, fbid, request);
    }
    // </editor-fold>
    
    /**
     * Performs an SQL call to check which CLUEID to begin generating the new 
     * clue set with.
     * 
     * @param incrementor The value to increase the multi part ClueID by per clue.
     * @return An integer corresponding to the current ClueID
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
     * @return The group id of the Facebook group.
     */
    protected String makeGroup(HttpServletRequest request) {
        String groupid = "";
      
        URL                 url;
        URLConnection       urlConn;
        DataOutputStream    printout;
        DataInputStream     input;
        
        String groupName = request.getParameter("groupName");
        String groupDesc = request.getParameter("groupDesc");
 
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

            String content = "name=" + groupName + "&description=" + groupDesc + "&access_token=" + ACCESS_TOKEN;
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
     * A helper method that sets up the architecture to generate the QR codes needed
     * for a new Huntsperson group.
     * 
     * Invokes makeQR(String, int) and insertClueIntoTable(HttpServletRequest, String, int, int)
     * 
     * @param groupNum 
     * @param intNum
     * @param fbid
     * @param request 
     */
    protected void genQRCode(int groupNum, int intNum, String fbid, HttpServletRequest request) {
        
        groupNum = this.parseClueID(groupNum);
        
        //TODO: devise weblink to encode in QR.
        //webLink must equal a get/post that will call the web server and check
        //whether the QRcode is valid or not.
        String webLink = "tempdata"; 
        String filepath = "";
        
        //First time run logic, checks whether the QR directory exists.
        if (!new File(QR_PATH).exists()) { 
            new File(QR_PATH).mkdirs();
        }
        
        for (int i = 0; i < intNum; i++)
        {
            //Code for generating QR codes for each clue goes here.
            groupNum++;
            
            try { filepath = this.makeQR(webLink, groupNum);
            } catch (IOException e) {
                this.logError(e, "genQRCode");
                return;
            } 
            
            this.insertClueIntoTable(request, filepath, fbid, groupNum, i);
        }
    }
    
    /**
     * Generates a QR code 
     * 
     * @param webLink The data to insert into the QR code.
     * @param groupNum 
     * @return
     * @throws IOException 
     */
    protected String makeQR(String webLink, int groupNum) throws IOException {
        FileOutputStream fout = null;
        String filepath = "";
        try {
            ByteArrayOutputStream out = QRCode.from(webLink).to(ImageType.PNG).stream();
            
            filepath = QR_PATH.concat(String.valueOf(groupNum).concat(".png"));
            fout = new FileOutputStream(new File(filepath));
            fout.write(out.toByteArray());
            fout.flush();
                
            Logger lgr = Logger.getLogger("makeQR");
            lgr.log(Level.INFO, "Created QRCode at {0}.", filepath);
            
        } catch (FileNotFoundException e) {
            this.logError(e, "makeQR");
            filepath = null;
                
        } catch (IOException e) {
            this.logError(e, "makeQR");
            filepath = null;
            
        } finally {
            if (fout != null) { fout.close(); }
        }
        return filepath;
    }
    
    /**
     * Inserts a clue passed in via HttpRequest into the database, associated with
     * a clue and a facebook group id.
     * 
     * @param request The HttpServletRequest object that originated this call.  
     * Contains the CLUE string in form data in "clue" + i format.
     * @param fbid The facebook group id that this clue is associated with.
     * @param groupNum The CLUEID of the given clue.
     * @param i The incrementor used to get clues from the request object.
     */
    protected void insertClueIntoTable(HttpServletRequest request, String filepath, String fbid, int groupNum, int i) {
        try {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
                
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Huntsperson");
                
            con = ds.getConnection();
            st = con.createStatement();
                
            String str = request.getParameter(("clue").concat(String.valueOf(i)));
            String insert = "INSERT INTO CLUETABLE (CLUEID,FBGROUPID,CLUE,QRPATH) ";
            String values = "VALUES (" + groupNum + ",'" + fbid + "','" + str + "','" + filepath + "')";
               
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
