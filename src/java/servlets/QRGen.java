/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servlets;

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

/**
 *
 * @author Ortin
 */
@WebServlet(name = "QRGen", urlPatterns = {"/QRGen"})
public class QRGen extends HttpServlet {

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
            this.setUpFBGroup();
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
     * @param request The Http Request object originating the request
     * @param response The Http Response object directed to the requesting object
     */
    protected void processClues(HttpServletRequest request, HttpServletResponse response) 
    {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Huntsperson");
            
            con = ds.getConnection();
            st = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            
            //Grabs values
            rs = st.executeQuery("SELECT MAX(CLUEID) FROM CLUETABLE");
            
            Logger lgr = Logger.getLogger("processClues");
            if (rs.isBeforeFirst())
            {
                lgr.log(Level.INFO, "SQL query SELECT MAX(CLUEID) FROM CLUETABLE returned null pointer.");
                
            }
        }
        catch (SQLException e)
        {
            Logger lgr = Logger.getLogger("processClues");
            lgr.log(Level.SEVERE, e.getMessage(), e);
        }
        catch (NamingException e)
        {
            Logger lgr = Logger.getLogger("processClues");
            lgr.log(Level.SEVERE, e.getMessage(), e);
        }
        finally
        {
            //I have no idea what should go here, if anything.
        }
        
        String strNum = request.getParameter("numClues");
        int intNum = Integer.parseInt(strNum);
        
        for (int i = 0; i < intNum; i++)
        {
            //Code for generating QR codes for each clue goes here.
        }
    }
    
    /**
     * Creates the Facebook group for a given Huntsperson 
     */
    protected void setUpFBGroup() {
        
    }
    
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
