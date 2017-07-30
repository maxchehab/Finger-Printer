// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.

let APPLICATIONID = "electron-example-3.7";
let LABEL = "Max's Computer";

var fingerprinter = require('finger-printer');


fingerprinter.on('addDevice', function(device) {
     console.log(device);
     if (!containsDevice(device.endpoint)) {
          devicesAvailable.push(device);
          updateDevices();
     }
});

fingerprinter.on('removeDevice', function(endpoint) {
     removeDevice(endpoint);
     updateDevices();
});


var selector = document.getElementById("phone-select");

var devicesAvailable = [];
updateDevices();

var scanIntervalID;

function startScan() {
     fingerprinter.findDevices();
     scanIntervalID = setInterval(fingerprinter.findDevices(), 10000);
}

function stopScan() {
     clearInterval(scanIntervalID)
}
startScan();


$('#register-button').click(function() {
     stopScan()
     startTransitionAnimation();
     fingerprinter.pair($('#phone-select').find(":selected").val(),
          APPLICATIONID,
          LABEL,
          $('#username').val(),
          saltGenerator(),
          function(success, data) {
               if (success) {
                    updateUsername(data.username);
                    successAnimation();
               } else if (data.message == "already paired") {
                    error("Device is already paired with this application. Maybe you want to login?");
                    cancelTransitionAnimation();
               } else if (data.message == "i am already connected") {
                    error("Device became unavailable.");
                    cancelTransitionAnimation();
               } else if (data.message == "close" || data.message == "error" || data.message == "ran pair") {
                    error("Device timed out. Please try again.");
                    cancelTransitionAnimation();
               } else {
                    error("Unexpected response. Please contact an administrator.");
                    cancelTransitionAnimation();
               }

               if(!success){
                    startScan();
               }
          });
});

$('#login-button').click(function() {
     stopScan()
     startTransitionAnimation();
     fingerprinter.authenticate($('#phone-select').find(":selected").val(),
          APPLICATIONID,
          function(success, data) {
               if (success) {
                    updateUsername(data.username);
                    successAnimation();
               } else if (data.message == "i do not know that applicationID") {
                    error("Device does not know this application. Maybe you want to register?");
                    cancelTransitionAnimation();
               } else if (data.message == "i am already connected") {
                    error("Device became unavailable.");
                    cancelTransitionAnimation();
               } else if (data.message == "close" || data.message == "error" || data.message == "ran authentication") {
                    error("Device timed out. Please try again.");
                    cancelTransitionAnimation();
               } else {
                    error("Unexpected response. Please contact an administrator.");
                    cancelTransitionAnimation();
               }

               if(!success){
                    startScan();
               }
          });
});

$('#logout').click(function() {
     logoutAnimation();
     startScan();
})


function updateUsername(username) {
     document.getElementById("welcome").innerHTML = "Welcome back <strong>" + username + "</strong>";
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
