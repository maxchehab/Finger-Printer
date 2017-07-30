var net = require('net');

var addDevice = function() {};
var removeDevice = function() {};
var success = function() {};

exports.on = function(command, callback) {
     switch (command) {
          case 'addDevice':
               addDevice = callback;
               break;
          case 'removeDevice':
               removeDevice = callback;
               break;
     }
}

exports.findDevices = function() {
     //Obtain the ip address
     var ip = require("ip");
     var keys = ip.address().split('.');

     /*This nested loop pings all local devices on the network.
     By finding the ip address and looping through all 768 possibilities
     it will successfuly loop through all available ip addressess.

     For example, if the current ip address is 10.0.1.10, the loop will
     ping every ip address between 10.0.0.0 and 10.0.2.256*/
     for (var i = 0; i < 2; i++) {
          for (var j = 0; j < 256; j++) {
               ping(keys[0] + "." + keys[1] + "." + i + "." + j);
          }
     }
}

function ping(endpoint) {
     //Create socket client.
     var client = new net.Socket();

     //Connect socket to port 61597
     client.connect(61597, endpoint);

     /*If the port is open, this function will determine if the endpoint
     hosts a suitable connection*/
     client.on('data', function(data) {
          //Collect and convert the data to JSON
          data = JSON.parse(data);

          //If {'success':true...} the device is hosting a Finger Printer server
          if (data.success) {
               //Callback addDevice() with device information
               addDevice && addDevice({
                    endpoint: endpoint,
                    hardwareID: data.hardwareID,
                    deviceName: data.deviceName
               });

               /*Sometimes the device will have a server but is busy with a connection.
               If this is the case, it will continue to try and connect with the device
               but will callback removeDevice()*/
          } else if (data.message == "i am already connected") {
               removeDevice && removeDevice({
                    endpoint: endpoint
               });
               ping(endpoint);
          }
          client.destroy();
     });

     //If the client fails, the socket is destroyed and removeDevice callback is called.
     client.on('error', function(err) {
          removeDevice && removeDevice({
               endpoint: endpoint
          });
          client.destroy();
     });
}

exports.authenticate = function authenticate(endpoint, applicationID, callback) {
     var client = new net.Socket();
     var success = false;

     client.connect(61597, endpoint);

     client.on('data', function(data) {
          data = JSON.parse(data);

          if (data.command == "knock-knock" && data.success) {
               var command = JSON.stringify({
                    applicationID: applicationID,
                    command: "authenticate"
               });
               client.write(command + "\n");
          } else if (data.command == "authenticate" && data.success) {
               success = true;
               callback && callback(false, data);
               client.destroy();
          } else if (data.command == "authenticate" && data.message == "i do not know that applicationID") {
               callback && callback("Device does not know this application. Maybe you want to register?");
               client.destroy();

          } else if (data.message == "i am already connected") {
               callback && callback("Device became unavailable.");
               client.destroy();
          } else if (data.command == "authenticate" && !data.success) {
               callback && callback("Authentication failed. Please make sure your fingerprint is saved to your device.");
               client.destroy();
          } else if (!data.success) {
               callback && callback("Request was invalid. Please contact an administrator.");
               client.destroy();
          }
     });

     client.on('close', function() {
          if (!success) {
               callback && callback("Device has timed out.");
          }
     });

     client.on('error', function(err) {
          callback && callback("Device has timed out.");
     });
}


exports.pair = function() {

}
