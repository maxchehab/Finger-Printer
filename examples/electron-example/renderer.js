// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.
var arp = require('arp-a');
var selector = document.getElementById("phone-select");

var ipAmount = 0;

arp.table(function(err, entry) {
     if (!!err) return console.log('arp: ' + err.message);
     if (!entry) return;

     console.log(entry.ip);

     selector.innerHTML += "<option value='" + entry.ip + "'>" + entry.ip + "</option>"

     if(ipAmount === 1){
          selector.innerHTML = "<option style='color:#4E546D !important' value='null'>Select a device.</option>" + selector.innerHTML;
          selector.style.color = "#4E546D";
     }

     ipAmount++;
});
