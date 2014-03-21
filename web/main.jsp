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
        <script type='text/javascript'>
        function addFields(){
            // Number of inputs to create
            var number = document.getElementById("clues").value;
            
            document.getElementById("numClues").value = number;
            /*
            var labels = document.getElementsByTagName("input");
            for( var i = 0; i < labels.length; i++ ){
                if( labels[i].outerHTML.indexOf('name="numClues"') > -1){
                    labels[i].innerHTML = number;
                }
            }*/
            // Container <div> where dynamic content will be placed
            var container = document.getElementById("container");
            // Clear previous contents of the container
            while (container.hasChildNodes()) {
                container.removeChild(container.lastChild);
            }
            for (i=0;i<number;i++){
                // Append a node with a random text
                container.appendChild(document.createTextNode("Clue " + (i+1)));
                // Create an <input> element, set its type and name attributes
                var input = document.createElement("input");
                input.type = "text";
                input.name = "clue" + i;
                container.appendChild(input);
                // Append a line break 
                container.appendChild(document.createElement("br"));
            }
            var submit = document.createElement("input");
            submit.type = "submit";
            submit.name = "action";
            submit.value = "submit";
            container.appendChild(submit);
        }
    </script>
    </head>
    <body>
        <h1>This is the main page for Huntsperson</h1>
            <form name="item" method="POST" action="QRGen">
                <input type="hidden" id="numClues" name="numClues" value="">
                <input type="text" id="clues" name="clues" value="">Number of Clues<br />
                <div id="container"/>
            </form>
        <a href="#" id="filldetails" onclick="addFields()">Fill Details</a>
    </body>
</html>
