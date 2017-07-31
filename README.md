# Finger-Printer

Finger Printer is a mobile application that can authenticate desktop applications using a device's fingerprint reader.

Download the app [here](https://play.google.com/store/apps/details?id=com.maxchehab.fingerprinter).

Checkout the npm repository [here](https://www.npmjs.com/package/finger-printer).



# Documentation
## Finger Printer Protocol

The following are global guidelines describing the Finger Printer protocol.

* The Finger Printer socket will timeout after 30 seconds.
* All Finger Printer sockets communicate on port `61597`.
* Only one application can connect to Finger Printer at a time.
* If not already connected with another device, Finger Printer will send a `knock-knock` response on connection.
* If already connected, Finger Printer will respond with `{"success":false,"message":"i am already connected"}` and disconnect.

## Requests

### `knock-knock`
`knock-knock` responds with a unique `hardwareID` for the device and `deviceName` describing the devices name to the network. If not already connected with another device, Finger Printer will automatically respond with a `knock-knock` response when a connection is initialized. 

**Request:**

```json
{
     "command": "knock-knock"
}
```

**Response:**

```json
{
     "success": true,
     "command": "knock-knock",
     "hardwareID": "9696a472835e0d3d",
     "deviceName": "SAMSUNG-SM-G930V"
}
```

### `pair`
A `pair` request will add a new user to the Finger Printer device's local database. The user's application will also be added to the local database if not done so already. A user can only be paired once.

**Parameters:**

* `applicationID` is a unique string used to identify an application for authentication requests. This is hidden from the user.

* `username` is the given username of the user.

* `label` is the name of the application. This is displayed to the user.

* `salt` is a random string that is included during the hashing process of the pairing. 

**Request:**

```json
{
     "command": "pair",
     "applicationID": "213af32",
     "label": "Example Application",
     "username": "user1234",
     "salt": "ejin2ns12"
}
```

**Successful response:**
```json
{
     "success": true,
     "username": "user1234",
     "command": "pair",
     "message": "ran pair",
     "uniqueKey": "BF5C9D78A667D8E8D8BD741422AB6027CA7E7A72F8FF3EAB9AF84CC32C841214",
     "hardwareID": "9696a472835e0d3d"
}
```
**Unsuccessful response (already paired):**
```json
{
     "success": false,
     "command": "pair",
     "message": "already paired"
}
```
**Treat the `uniqueKey` and `hardwareID` as a password. Hash and salt it in your database. Every successful authentication response will contain an *__identical__* `uniqueKey` and `hardwareID`.**

### `authenticate`
An `authenticate` request will authenticate an application. Because Finger Printer can contain multiple users per application, the username to authenticate with will be selected by the user in the app.

**Parameters:**

* `applicationID` is a unique string used to identify an application for authentication requests. This is hidden from the user.

**Request:**

```json
{
     "command": "authenticate",
     "applicationID": "213af32"
}
```

**Successful Response:**
```json
{
     "success": true,
     "username": "user1234",
     "command": "authenticate",
     "message": "ran authentication",
     "uniqueKey": "BF5C9D78A667D8E8D8BD741422AB6027CA7E7A72F8FF3EAB9AF84CC32C841214",
     "hardwareID": "9696a472835e0d3d"
}
```


**Unsuccessful Response: (Unknown applicationID)**
```json
{
     "success": false,
     "command": "authenticate",
     "message": "i do not know that applicationID"
}
```

**Treat the `uniqueKey` and `hardwareID` as a password. Hash and salt it in your database. Every successful authentication response will contain an *__identical__* `uniqueKey` and `hardwareID`.**

## Other Responses
**Unkown Command:**
```json
{
     "success": false,
     "message": "i do not understand that command"
}
```
**Internal Error:**
```json
{
     "success": false,
     "message": "java.lang.IllegalStateException: Not a JSON Object...'"
}
```
