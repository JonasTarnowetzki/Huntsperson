/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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