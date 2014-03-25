/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
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

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet QRGen</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet QRGen at " + request.getContextPath() + "</h1>");
            
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

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
        processRequest(request, response);
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
        
        if (strAction.equalsIgnoreCase("submit"))
        {
            processRequest(request, response);
            this.processClues(request, response);
        }
        else
        {
            
        }
        processRequest(request, response);
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
        
        int groupNum; 
        int groupIncrementor = 100; //increments the group number of CLUEID by one
        
        groupNum = this.getCurClueID(groupIncrementor);
        
        //Grabs form data for loops made later.
        String strNum = request.getParameter("numClues");
        int intNum = Integer.parseInt(strNum);
        
        try {
            this.genQRCode(groupNum, intNum);
        }
        catch (IOException e)
        {
            this.logError(e, "processClues");
        }
    }
    
    /**
     * Performs an SQL call to check which CLUEID to begin generating the new 
     * clue set with.
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
            if (!rs.first()) //Database empty
            {
                lgr.log(Level.INFO, "SQL query SELECT MAX(CLUEID) FROM CLUETABLE returned null pointer.");
            }
            else
            {
                num = rs.getInt(1);
                lgr.log(Level.INFO, "SQL query SELECT MAX(CLUEID) FROM CLUETABLE returned {0}", num);
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
        // </editor-fold>
        
        return num;
    }
    
    
    protected void genQRCode(int groupNum, int intNum) throws IOException {
        
        groupNum = this.parseClueID(groupNum);
        
        //TODO: devise weblink to encode in QR.
        //webLink must equal a get/post that will call the web server and check
        //whether the QRcode is valid or not.
        String webLink = "tempdata"; 
        
        //First time run logic, checks whether the QR directory exists.
        if (!new File(QR_PATH).mkdirs()) { throw new IOException(); }
        
        for (int i = 0; i < intNum; i++)
        {
            //Code for generating QR codes for each clue goes here.
            ByteArrayOutputStream out = QRCode.from(webLink)
                                        .to(ImageType.PNG).stream();
            groupNum++;
            //TODO: Refactor this try/catch for readability.
            try {
                
                FileOutputStream fout = new FileOutputStream(
                        new File(QR_PATH.concat(String.valueOf(groupNum).concat(".png"))));
 
                fout.write(out.toByteArray());
 
                fout.flush();
                fout.close();
 
            } catch (FileNotFoundException e) {
                this.logError(e, "genQRCode");
            } catch (IOException e) {
                this.logError(e, "genQRCode");
            } finally {
                
            }
                //Also include code to assign data to database.
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
     * Logs a generic exception
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
