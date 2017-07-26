// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.

let APPLICATIONID = "electron-example-3.5";
let LABEL = "Electron Example 2.0";

var net = require('net');

var selector = document.getElementById("phone-select");

var devicesAvailable = [];


var ip = require("ip");

var keys = ip.address().split('.');

var scanIntervalID;
var scanning = false;

function startScan() {
     scan();
     scanIntervalID = setInterval(scan, 10000);
}

function scan() {
     for (var i = 0; i < 2; i++) {
          for (var j = 0; j < 256; j++) {
               pingDevice(keys[0] + "." + keys[1] + "." + i + "." + j);
          }
     }
}

function stopScan() {
     clearInterval(scanIntervalID)
}
startScan();




$('#register-button').click(function() {
     stopScan()
     startTransitionAnimation();
     pairDevice($('#phone-select').find(":selected").val());
});

$('#login-button').click(function() {
     stopScan()
     startTransitionAnimation();
     authenticateDevice($('#phone-select').find(":selected").val());
});

$('#logout').click(function() {
     logoutAnimation();
     startScan();
})




function authenticateDevice(endpoint) {
     console.log("authenticating : " + endpoint);

     var client = new net.Socket();
     var authenticateSuccess = false;

     client.connect(61597, endpoint, function() {
          console.log('Connected');
     });

     client.on('data', function(data) {
          console.log(endpoint + ' : Received : ' + data);
          data = JSON.parse(data);

          if (data.command == "knock-knock" && data.success) {
               var command = JSON.stringify(new authenticateCommand(APPLICATIONID));
               console.log("authenticating : " + command);
               client.write(command + "\n");
          } else if (data.command == "authenticate" && data.success) {
               successAnimation();
               stopScan()
               updateUsername(data.username);
               authenticateSuccess = true;
               client.destroy();
          } else if (data.command == "authenticate" && data.message == "i do not know that applicationID") {
               error("Device does not know this application. Maybe you want to register?");
               console.log("not recoginized");
               cancelTransitionAnimation();
               client.destroy();

          } else if (data.message == "i am already connected") {
               console.log("already connected")
               cancelTransitionAnimation();
               client.destroy();
               stopScan();
               error("Device became unavailable.");
          } else if (data.command == "authenticate" && !data.success) {
               console.log("authentication failed");
               cancelTransitionAnimation();
               client.destroy();
               devicesAvailable = [];

               error("Authentication failed. Please make sure your fingerprint is saved to your device.");
          } else if (!data.success) {
               console.log(data.message);
               cancelTransitionAnimation();
               client.destroy();
               error("Request was invalid. Please contact an administrator.");
          }
     });

     client.on('close', function() {
          console.log(endpoint + ' : Connection closed');
          if (!authenticateSuccess) {
               error("Device has timed out.");
               cancelTransitionAnimation();
               startScan()
          }
     });

     client.on('error', function(err) {
          console.log(endpoint + ' : ' + err);
          error("Device has timed out.");
          cancelTransitionAnimation();
          startScan()
     });
}

function updateUsername(username){
     document.getElementById("welcome").innerHTML = "Welcome back <strong>" + username + "</strong>";
}

function pairDevice(endpoint) {
     console.log("pairing : " + endpoint);

     var client = new net.Socket();
     var pairSuccess = false;


     client.setTimeout(30000);
     client.on('timeout', () => {
          client.destroy();
     });
     client.connect(61597, endpoint, function() {
          console.log('Connected');
     });

     client.on('data', function(data) {
          console.log(endpoint + ' : Received : ' + data);
          data = JSON.parse(data);

          if (data.command == "knock-knock" && data.success) {
               var command = JSON.stringify(new pairCommand(APPLICATIONID, LABEL, $('#username').val()));
               console.log("pairing : " + command);
               client.write(command + "\n");
          } else if (data.command == "pair" && data.success) {
               successAnimation();
               stopScan()
               updateUsername(data.username);
               pairSuccess = true;
               client.destroy();
          } else if (data.command == "pair" && data.message == "already paired") {
               error("Device is already paired with this application. Maybe you want to login?");
               console.log("already paired");
               cancelTransitionAnimation();
               client.destroy();

          } else if (data.message == "i am already connected") {
               console.log("already connected")
               cancelTransitionAnimation();
               client.destroy();
               startScan();
               error("Device became unavailable.");
          } else if (data.command == "pair" && !data.success) {
               console.log("authentication failed");
               cancelTransitionAnimation();
               client.destroy();
               error("Authentication failed. Please make sure your fingerprint is saved to your device.");
          } else if (!data.success) {
               console.log(data.message);
               cancelTransitionAnimation();
               client.destroy();
               error("Request was invalid. Please contact an administrator.");
          }
     });

     client.on('close', function() {
          console.log(endpoint + ' : Connection closed');
          if (!pairSuccess) {
               error("Device has timed out.");
               cancelTransitionAnimation();
               startScan()
          }
     });

     client.on('error', function(err) {
          console.log(endpoint + ' : ' + err);
          error("Device has timed out.");
          cancelTransitionAnimation();
          startScan()
     });
}


function pingDevice(endpoint) {
     console.log("pinging : " + endpoint);

     var client = new net.Socket();



     client.connect(61597, endpoint, function() {
          console.log('Connected');
     });

     client.on('data', function(data) {
          console.log(endpoint + ' : Received: ' + data);
          data = JSON.parse(data);
          if (data.success) {
               if (!containsDevice(endpoint)) {
                    devicesAvailable.push({
                         endpoint: endpoint,
                         hardwareID: data.hardwareID,
                         deviceName: data.deviceName
                    });
                    updateDevices()
               }
          } else if (data.message == "i am already connected") {
               removeDevice(endpoint);
               pingDevice(endpoint);
          }
          client.destroy();
     });

     client.on('close', function() {
          console.log('Connection closed');
     });

     client.on('error', function(err) {
          removeDevice(endpoint);
          console.log(endpoint + ' : ' + err);
     });
}

function containsDevice(endpoint) {
     for (var i = 0; i < devicesAvailable.length; i++) {
          if (devicesAvailable[i].endpoint === endpoint) {
               return true;
          }
     }
     return false;
}

function removeDevice(endpoint) {
     for (var i = 0; i < devicesAvailable.length; i++) {
          if (devicesAvailable[i].endpoint === endpoint) {
               devicesAvailable.splice(i, 1);
               break;
          }
     }
     updateDevices();
}

function updateDevices() {
     var value = $(selector).val();
     console.log("value " + value);
     selector.innerHTML = "";
     for (var i = 0; i < devicesAvailable.length; i++) {
          selector.innerHTML += "<option value='" + devicesAvailable[i].endpoint + "'>" + devicesAvailable[i].deviceName + "</option>"
     }
     if (devicesAvailable.length === 0) {
          selector.innerHTML = "<option value='null'>Searching...</option>";
          hidePhoneTick();
     } else if (devicesAvailable.length === 1) {
          showPhoneTick();
     }
     if (devicesAvailable.length > 1) {
          selector.innerHTML = "<option value='device' style='color:#4E546D  !important'>Select a device.</option>" + selector.innerHTML;
          if ($("#phone-select option[value='" + value + "']").length > 0 && $(selector).val != null) {
               $(selector).val(value);
               console.log("HAS VALUE");
          } else {
               $(selector).val('device');
               selector.style.color = "#4E546D";
               hidePhoneTick();
          }
     }



}

function pairCommand(applicationID, label, username) {
     this.applicationID = applicationID;
     this.username = username;
     this.command = "pair";
     this.label = label;
     this.salt = saltGenerator();
}

function authenticateCommand(applicationID) {
     this.applicationID = applicationID;
     this.command = "authenticate";
}

function saltGenerator() {
     return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
}


function error(message, duration, bgColor, txtColor, height) {

     /*set default values*/
     duration = typeof duration !== 'undefined' ? duration : 4000;
     bgColor = typeof bgColor !== 'undefined' ? bgColor : "#35394a";
     txtColor = typeof txtColor !== 'undefined' ? txtColor : "#DC6180";
     height = typeof height !== 'undefined' ? height : 40;
     /*create the notification bar div if it doesn't exist*/
     if ($('#notification-bar').size() == 0) {
          var HTMLmessage = "<div class='notification-message' style='text-align:center; line-height: " + height + "px;'> " + message + " </div>";
          $('body').prepend("<div id='notification-bar' style='display:none; width:100%; height:" + height + "px; background-color: " + bgColor + "; position: fixed; z-index: 100; color: " + txtColor + ";border-bottom: 1px solid " + txtColor + ";'>" + HTMLmessage + "</div>");
     }
     /*animate the bar*/
     $('#notification-bar').slideDown(function() {
          setTimeout(function() {
               $('#notification-bar').slideUp(function() {});
          }, duration);
     });
}
