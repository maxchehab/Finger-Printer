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

     //Create client socket.
     var client = new net.Socket();
     var success = false;

     //Connect socket to port
     client.connect(61597, endpoint);

     client.on('data', function(data) {
          //Retrieve and convert data to json
          data = JSON.parse(data);

          /*By default, the application will return the knock-knock response.
          If we receive this response we need to continue the authentication
          request.*/
          if (data.command == "knock-knock" && data.success) {
               //Create command in json.
               var command = JSON.stringify({
                    applicationID: applicationID,
                    command: "authenticate"
               });
               //Write command to socket stream.
               client.write(command + "\n");

               /*if the response is for the authentication and success is true,
               the user successfuly authenticated.*/
          } else if (data.command == "authenticate" && data.success) {
               success = true;
               callback && callback(true, data);
               client.destroy();

               //All other if statements check specific errors.
          } else if (data.command == "authenticate" && data.message == "i do not know that applicationID") {
               callback && callback(false, data);
               client.destroy();

          } else if (data.message == "i am already connected") {
               callback && callback(false, data);
               client.destroy();
          } else if (data.command == "authenticate" && !data.success) {
               callback && callback(false, data);
               client.destroy();
          } else if (!data.success) {
               callback && callback(false, data);
               client.destroy();
          }
     });

     client.on('close', function() {
          if (!success) {
               callback && callback(false, {
                    message: "close"
               });
          }
     });

     client.on('error', function(error) {
          callback && callback(false, {
               message: "error",
               error: error
          });
     });
}

exports.pair = function(endpoint, applicationID, label, username, salt, callback) {
     var client = new net.Socket();
     var success = false;

     client.connect(61597, endpoint);

     client.on('data', function(data) {
          data = JSON.parse(data);

          if (data.command == "knock-knock" && data.success) {
               var command = JSON.stringify({
                    applicationID: applicationID,
                    username: username,
                    command: "pair",
                    label: label,
                    salt: salt
               })
               client.write(command + "\n");
          } else if (data.command == "pair" && data.success) {
               callback && callback(true, data);
               success = true;
               client.destroy();
          } else if (data.command == "pair" && data.message == "already paired") {
               callback && callback(false, data);
               client.destroy();
          } else if (data.message == "i am already connected") {
               callback && callback(false, data);
               client.destroy();
          } else if (data.command == "pair" && !data.success) {
               callback && callback(false, data);
               client.destroy();
          } else if (!data.success) {
               client.destroy();
               callback && callback(false, data);
          }
     });

     client.on('close', function() {
          if (!success) {
               callback && callback(false, {
                    message: "close"
               });
          }
     });

     client.on('error', function(error) {
          callback && callback(false, {
               message: "error",
               error: error
          });
     });
}
