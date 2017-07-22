# Finger-Printer
Finger Printer is a library and Android application that utilizes mobile devices fingerprint reader to authenticate users on a computer.

Communication with Finger Printer is via a simple socket server allowing virtually any language and platform take advantage of a phones fingerprint reader.

# How to use.
## The following is a reference on communication with Finger Printer.
##### I will try to update this frequently, but it may be out of date.

### Locating the local address (`knock-knock`)
The first step to communicating with Finger Printer is locating the phone's local address. As of writting, the best way to do this is generate a list of local addresses on your network.
```bash
~$ arp -a
gateway (10.0.1.1) at 5c:96:9d:6e:ba:75 [ether] on eno1
? (10.0.1.8) at b8:e9:37:2e:c4:a8 [ether] on eno1
? (172.28.128.7) at 08:00:27:03:08:29 [ether] on vboxnet0
? (10.0.1.5) at d4:c9:ef:ec:e9:83 [ether] on eno1
? (10.0.1.9) at b8:e9:37:bf:26:34 [ether] on eno1
? (10.0.1.31) at 8c:f5:a3:70:b9:46 [ether] on eno1
```
and connect to each address. If the address is indeed the endpoint to a FingerPrinter device it will return a message
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
                      or
{"success":false,"message":"i am already connected"}
Connection closed by foreign host.
```
If the latter response is returned, Finger Printer is busy with another application. I suggest saving the hardwareID returned and the endpoint used. This could be useful for automatically locating the correct device next time.
Once connected, if you wish to receive the hardwareID again just run the `knock-knock` command.
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
{"command":"knock-knock"}
```

### Pairing a new Finger Printer (`pair`)
The next step to authenticating your application, is to pair the newly found Finger Printer device with your `applicationID`. An `applicationID` is simply a string stored in the Finger Printer internal database. It is used to identify your application upon an authentication request. I suggest creating a random applicationID, legibility is not of issue here (users will never see your applicationID), and applicationID duplicates could become ugly. Your `applicationID` must be the same across all devices (there is nothing stopping you, but it is best practice).
After creating your `applicationID` you must create your application `label`. The `label` is a user-friendly string of your application's name. 
Finally, create a random `salt`. I suggest using a unique `salt` for each Finger Printer.
Once you have created your `applicationID`, `label`, and `salt` you may pair with the Finger Printer.
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
{"command":"pair","applicationID":"75cc98cb","label":"Awesome Application","salt":"63cdd4a27657"}
{"success":true,"message":"paired","uniqueKey":"10633D15A87AE1910193FEC20889A2D8E5D5E2CF91715EB97E6BCC7DD998F12A","hardwareID":"9696a472835e0d3d"}
```

Treat the `uniqueKey` as a password. Hash and salt it in your database if you have one. If Finger Printer authenticated correctly it will always return the uniqueKey.

If your application is already paired with the Finger Printer this will be returned.
```bash
{"success":false,"message":"already paired"}
```

### Authenticating with Finger Printer (`authenticate`)
Once you have connected and paired with the Finger Printer device you may send an authentication request.
When you send an authentication request a notification appears on the device. Only when the user selects the notification and authenticates with their fingerprint, a response is sent. An authentication request may take some time to be returned, it relies on the user's fingerprint!

A successful authentication request:
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
{"command":"authenticate","applicationID":"75cc98cb"}
{"success":true,"message":"authenticating","uniqueKey":"10633D15A87AE1910193FEC20889A2D8E5D5E2CF91715EB97E6BCC7DD998F12A"}
```
An unsuccessful authentication request:
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
{"command":"authenticate","applicationID":"75cc98cb"}
{"success":false,"message":"authenticating","uniqueKey":"10633D15A87AE1910193FEC20889A2D8E5D5E2CF91715EB97E6BCC7DD998F12A"}
```
An unsuccessful authentication request (not paired)
```bash
~$ telnet 10.0.1.31 61597
Trying 10.0.1.31...
Connected to 10.0.1.31.
Escape character is '^]'.
{"success":true,"hardwareID":"9696a472835e0d3d"}
{"command":"authenticate","applicationID":"75cc98cb"}
{"success":false,"message":"i do not know that applicationID"}
```



