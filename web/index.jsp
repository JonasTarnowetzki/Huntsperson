<%-- 
    Document   : index
    Created on : 18-Mar-2014, 12:39:29 PM
    Author     : Jonas Tarnowetzki
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Huntsperson</title>
        <!--<link rel="stylesheet" type="text/css" href="main_style.css" />-->
        <script src="hunt.js"></script>
    </head>
    <body>
        
        <!--
        Below we include the Login Button social plugin. This button uses the JavaScript SDK to
        present a graphical Login button that triggers the FB.login() function when clicked. -->

        <fb:login-button show-faces="true" width="200" max-rows="1"></fb:login-button>
        <h1>Welcome to Huntsperson!</h1>
            <form name="item" method="POST" action="QRGen">
                <p1 id="p1">
                    <input type="text" id="groupName" name="groupName" value="">Group Name<br />
                    <input type="text" id="groupDesc" name="groupDesc" value="">Group Description<br />
                </p1>
                <p2 id="p3" class="hidden">
                    <input type="text" id="clues" name="clues" value="">Number of Clues<br /> 
                </p2>
                <input type="hidden" id="numClues" name="numClues" value="">
                <div id="container"/>
            </form>
            <p2 id="p4" class="hidden"><a href="#" id="filldetails" onclick="addFields()">Generate Clue Form</a></p2>
    </body>
</html>