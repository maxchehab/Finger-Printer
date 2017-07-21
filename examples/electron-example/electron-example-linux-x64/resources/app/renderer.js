// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
var arp = require('arp-a');
var net = require('net');


var selector = document.getElementById("phone-select");

var ipAmount = 0;

arp.table(function(err, entry) {
     if (!!err) return console.log('arp: ' + err.message);
     if (!entry) return;

     knock_knock(entry.ip);
});

function knock_knock(endpoint) {
     console.log("testing connection: " + endpoint);

     var client = new net.Socket();
     client.connect(61597, endpoint, function() {
          console.log('Connected');
     });

     client.on('data', function(data) {
          console.log('Received: ' + data);
          data = JSON.parse(data);
          if(data.success){
               add_device(data.deviceName);
          }
          client.destroy();
     });

     client.on('close', function() {
          console.log('Connection closed');
     });
}

function add_device(name){
     if(ipAmount === 0){
          selector.innerHTML = "<option value='" + name + "'>" + name + "</option>"
     }else{
          selector.innerHTML += "<option value='" + name + "'>" + name + "</option>"

     }


     if (ipAmount === 1) {
          selector.innerHTML = "<option style='color:#4E546D !important' value='null'>Select a device.</option>" + selector.innerHTML;
          selector.style.color = "#4E546D";
     }
     $('#phone-select').change();

     ipAmount++;
}
