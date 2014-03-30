<%-- 
    Document   : main
    Created on : 18-Mar-2014, 5:32:06 PM
    Author     : Ortin
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Huntsperson - Main</title>
        <link rel="stylesheet" type="text/css" href="main_style.css" />
        <script src="hunt.js"></script>
    </head>
    <body>
        <h1>This is the main page for Huntsperson</h1>
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
        <p1 id="p2"><a href="#" id="postGroup" onclick="postGroup()">Create Huntsperson Group on Facebook</a></p1>
        <p2 id="p4" class="hidden"><a href="#" id="filldetails" onclick="addFields()">Generate Clue Form</a></p2>
    </body>
</html>
