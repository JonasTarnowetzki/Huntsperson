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
        <div id="fb-root"></div>
        <script>
        var appid = "480920812033752";

            window.fbAsyncInit = function() {
            FB.init({
                appId      : appid,
                status     : true, // check login status
                cookie     : true, // enable cookies to allow the server to access the session
                xfbml      : true  // parse XFBML
            });

            // Here we subscribe to the auth.authResponseChange JavaScript event. This event is fired
            // for any authentication related change, such as login, logout or session refresh. This means that
            // whenever someone who was previously logged out tries to log in again, the correct case below 
            // will be handled. 
            FB.Event.subscribe('auth.authResponseChange', function(response) {
                // Here we specify what we do with the response anytime this event occurs. 
                if (response.status === 'connected') {
                    // The response object is returned with a status field that lets the app know the current
                    // login status of the person. In this case, we're handling the situation where they 
                    // have logged in to the app.
                    testAPI();
                } else if (response.status === 'not_authorized') {
                    // In this case, the person is logged into Facebook, but not into the app, so we call
                    // FB.login() to prompt them to do so. 
                    // In real-life usage, you wouldn't want to immediately prompt someone to login 
                    // like this, for two reasons:
                    // (1) JavaScript created popup windows are blocked by most browsers unless they 
                    // result from direct interaction from people using the app (such as a mouse click)
                    // (2) it is a bad experience to be continually prompted to login upon page load.
                    FB.login(function(response){}, {scope: 'publish_actions,user_groups'});
                    var access_token = response.authResponse.accessToken;
                    document.getElementById("userToken").value = access_token;
                } else {
                    // In this case, the person is not logged into Facebook, so we call the login() 
                    // function to prompt them to do so. Note that at this stage there is no indication
                    // of whether they are logged into the app. If they aren't then they'll see the Login
                    // dialog right after they log in to Facebook. 
                    // The same caveats as above apply to the FB.login() call here.
                    FB.login(function(response){}, {scope: 'publish_actions,user_groups'});
                    var access_token = response.authResponse.accessToken;
                    document.getElementById("userToken").value = access_token;
                }
            });
        };

        // Load the SDK asynchronously
        (function(d){
            var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
            if (d.getElementById(id)) {return;}
            js = d.createElement('script'); js.id = id; js.async = true;
            js.src = "//connect.facebook.net/en_US/all.js";
            ref.parentNode.insertBefore(js, ref);
        }(document));

        // Here we run a very simple test of the Graph API after login is successful. 
        // This testAPI() function is only called in those cases. 
        function testAPI() {
            console.log('Welcome!  Fetching your information.... ');
            console.log('Checking permissions....');
            FB.getLoginStatus(function(response) {
                // the user is logged in and has authenticated your
                // app, and response.authResponse supplies
                // the user's ID, a valid access token, a signed
                // request, and the time the access token 
                // and signed request each expire
                var uid = response.authResponse.userID;
                var accessToken = response.authResponse.accessToken;
                document.getElementById("userToken").value = accessToken;
            });
                FB.api('/me', function(response) {
                    console.log('Good to see you, ' + response.name + '.');
                    console.log('User ID: ' + response.id + '.');
                    document.getElementById("userID").value = response.id;
                });
        }
        </script>
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
                <input type="hidden" id="userToken" name="userToken" value="">
                <input type="hidden" id="userID" name="userID" value="">
                <div id="container"/>
            </form>
            <p2 id="p4" class="hidden"><a href="#" id="filldetails" onclick="addFields()">Generate Clue Form</a></p2>
    </body>
</html>