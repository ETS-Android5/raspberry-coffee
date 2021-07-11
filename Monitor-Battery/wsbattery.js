"use strict";  // http://ejohn.org/blog/ecmascript-5-strict-mode-json-and-more/

let simulation = true; // Set to false for real measurement.

/**
 * WebSocket server for Battery Monitoring sample
 * Static requests must be prefixed with /web/, like in http://machine:9876/web/console.html
 *
 * When a string is received, it is re-broadcasted to all the connected WS clients.
 */

// Optional. You will see this name in eg. 'ps' or 'top' command
process.title = 'node-battery';

if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str) {
        return this.indexOf(str) == 0;
    };
}

if (typeof String.prototype.endsWith != 'function') {
    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };
}

console.log('To stop: Ctrl-C, or enter "quit" + [return] here in the console');
console.log("Usage: node " + __filename + " -verbose -port:XXXX");

// Port where we'll run the websocket server on
let port = 9876;
let verbose = false;

if (process.argv.length > 2) {
    for (var i=2; i<process.argv.length; i++) {
        if (process.argv[i] === "-verbose") {
            verbose = true;
        } else if (process.argv[i].startsWith("-port:")) {
            port = parseInt(process.argv[i].substr("-port:".length));
        }
    }
}


// websocket AND http servers
let webSocketServer = require('websocket').server;
let http = require('http');
let fs = require('fs');

let handler = (req, res) => {
    let respContent = "";
    if (verbose === true) {
        console.log("Speaking HTTP from " + __dirname);
        console.log("Server received an HTTP Request:\n" + req.method + "\n" + req.url + "\n-------------");
        console.log("ReqHeaders:" + JSON.stringify(req.headers, null, '\t'));
        console.log('Request:' + req.url);
        let prms = require('url').parse(req.url, true);
        console.log(prms);
        console.log("Search: [" + prms.search + "]");
        console.log("-------------------------------");
    }
    if (req.url === "/favicon.ico") {
        fs.readFile(__dirname + '/web/favicon.ico',
            (err, data) => {
                if (err) {
                    res.writeHead(500);
                    return res.end('Error loading ' + resource);
                }
                res.writeHead(200, {'Content-Type': "image/ico"});
                res.end(data, 'binary');
            });
    } else if (req.url.startsWith("/web/")) { // Static resource
        var resource = req.url.substring("/web/".length);
        if (resource.indexOf("?") > -1) {
            resource = resource.substring(0, resource.indexOf("?"));
        }
        console.log('Loading static ' + req.url + " (" + resource + ")");
        fs.readFile(__dirname + '/web/' + resource,
            (err, data) => {
                if (err) {
                    res.writeHead(500);
                    return res.end('Error loading ' + resource);
                }
                // if (verbose)
                //   console.log("Read resource content:\n---------------\n" + data + "\n--------------");
                let contentType = "text/html";
                if (resource.endsWith(".css")) {
                    contentType = "text/css";
                } else if (resource.endsWith(".html")) {
                    contentType = "text/html";
                } else if (resource.endsWith(".xml")) {
                    contentType = "text/xml";
                } else if (resource.endsWith(".js")) {
                    contentType = "text/javascript";
                } else if (resource.endsWith(".jpg")) {
                    contentType = "image/jpg";
                } else if (resource.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (resource.endsWith(".png")) {
                    contentType = "image/png";
                } else if (resource.endsWith(".ico")) {
                    contentType = "image/ico";
                }

                res.writeHead(200, {'Content-Type': contentType});
                //  console.log('Data is ' + typeof(data));
                if (resource.endsWith(".jpg") ||
                    resource.endsWith(".gif") ||
                    resource.endsWith(".ico") ||
                    resource.endsWith(".png")) {
                    //  res.writeHead(200, {'Content-Type': 'image/gif' });
                    res.end(data, 'binary');
                } else {
                    res.end(data.toString().replace('$PORT$', port.toString())); // Replace $PORT$ with the actual port value.
                }
            });
    } else if (req.url === "/") {
        if (req.method === "POST") {
            let data = "";
            console.log("---- Headers ----");
            for (let item in req.headers) {
                console.log(item + ": " + req.headers[item]);
            }
            console.log("-----------------");

            req.on("data", (chunk) => {
                data += chunk;
            });

            req.on("end", () => {
                console.log("POST request: [" + data + "]");
                res.writeHead(200, {'Content-Type': 'application/json'});
                let status = {'status':'OK'};
                res.end(JSON.stringify(status));
            });
        }
    } else {
        console.log("Unmanaged request: [" + req.url + "]");
        //console.log(">>> " + JSON.stringify(req, null, 2));
        respContent = "Response from " + req.url;
        res.writeHead(404, {'Content-Type': 'text/plain'});
        res.end(); // respContent);
    }
}; // HTTP Handler


/**
 * Global variables
 */
// list of currently connected clients (users)
let clients = [ ];

/**
 * HTTP server
 */
let server = http.createServer(handler);

server.listen(port, () => {
    console.log((new Date()) + " Server is listening on port " + port);
    console.log("Point your browser to [http://localhost:" + port + "/web/adc.one.html]");
});

/**
 * WebSocket server
 */
let wsServer = new webSocketServer({
    // WebSocket server is tied to a HTTP server. WebSocket request is just
    // an enhanced HTTP request. For more info http://tools.ietf.org/html/rfc6455#page-6
    httpServer: server
});

// This callback function is called every time someone
// tries to connect to the WebSocket server
wsServer.on('request', (request) => {
    console.log((new Date()) + ' Connection from origin ' + request.origin + '.');

    // accept connection - you should check 'request.origin' to make sure that
    // client is connecting from your website
    // (http://en.wikipedia.org/wiki/Same_origin_policy)
    let connection = request.accept(null, request.origin);
    clients.push(connection);
    console.log((new Date()) + ' Connection accepted.');

    // user sent some message
    connection.on('message', (message) => {
        if (verbose) {
        //  console.log("On Message:" + JSON.stringify(message));
            console.log("On Message:", message);
        }
        if (message.type === 'utf8') {
            // accept only text
      //    console.log((new Date()) + ' Received Message: ' + message.utf8Data);
      //    console.log("Rebroadcasting: " + message.utf8Data);
            for (let i=0; i < clients.length; i++) {
                clients[i].sendUTF(message.utf8Data); // Just re-broadcast.
            }
        }
    });

    // user disconnected
    connection.on('close', (code) => {
        console.log((new Date()) + ' Connection closed.');
        let nb = clients.length;
        for (let i=0; i<clients.length; i++) {
            if (clients[i] === connection) {
                clients.splice(i, 1);
                break;
            }
        }
        if (verbose) {
            console.log("We have (" + nb + "->) " + clients.length + " client(s) connected.");
        }
    });
});

let done = () => {
    console.log("\nBye now!");
    process.exit();
};

process.on('SIGINT', done); // Ctrl C
process.stdin.resume();
process.stdin.setEncoding('utf8');

process.stdin.on('data', (text) => {
    // console.log('received data:', util.inspect(text));
    if (text.startsWith('quit')) {
        done();
    }
});

let simulate = () => {
    let fakedVolt = 10 + (Math.random() * 4);
    console.log("Faking ", fakedVolt);
    for (let i=0; i < clients.length; i++) {
        clients[i].sendUTF(fakedVolt.toFixed(2)); // Just re-broadcast.
    }
};

if (simulation) {
    console.log("--- SIMULATING ---");
    setInterval(simulate, 1000);
}
