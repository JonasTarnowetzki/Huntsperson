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
        <script type='text/javascript'>
            
        var appid = "480920812033752"; //Grabbed this from the Facebook API app page
        /**
         * Generates the number of text fields specified by Number of Clues field
         * and assigns them ids for future use.
         * @returns {undefined}
         */
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
                // Append a node with the clue number
                container.appendChild(document.createTextNode("Clue " + (i+1)));
                // Create an <input> element, set its type and name attributes
                var input = document.createElement("input");
                input.type = "text";
                input.name = "clue" + i;
                input.size = "64";
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
        
        /**
         * Creates a 
         * @returns {undefined}
         */
        function postGroup(){
            
            var name = document.getElementById("groupName").value;
            var groupDesc = document.getElementById("groupDesc").value;
            
            //Checks whether fields are empty or not.
            if (name==="" || groupDesc==="") {
                window.alert("Both the group name field and the group description fields must be filled.");
                return;
            }

            /* make the API call */
            /*
            FB.api(
                "/" + appid + "/groups",
                "POST",
                {
                    "object": {
                        "name"          : name,
                        "description"   : groupDesc
                    }
                },
                function (response) {
                    if (response && !response.error) {
                        // handle the result
                    }
                    else {
                        window.alert("Facebook creation failed with error: " + response.error);
                    }
                }
            ); */
        
            document.getElementById("p1").style.visibility="hidden";
            document.getElementById("p2").style.display="none";
            document.getElementById("p3").style.visibility="visible";
            document.getElementById("p4").style.visibility="visible";
        }
    </script>
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
