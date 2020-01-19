var http = require('http');
var sqlite3 = require('sqlite3');
var io = require('socket.io');
var sd = require('silly-datetime');

var MAC = [];
var MAC2 = []
var FW_ver = [];
var FW_ver2 = [];
var CheckFW = [];
var CheckFW2 = [];
var Renew = [];
var Renew2 = [];
var Time = [];
var Time2 = [];
var Product = [];
var Product2 = [];
var SetupCode = [];
var SetupCode2 = [];
var SerialNumber = [];
var SerialNumber2 = [];
var count = 0;
var count2 = 0
var SystemTime = 0;

const port = 3000;
const hostname = '127.0.0.1';

function getNowTime(){
    SystemTime = sd.format(new Date(), 'YYYY-MM-DD HH:mm:ss');
}

function dbStart() {
    //mac FW_ver CheckFW Renew date
    var db = new sqlite3.Database('../paper/Ameba.db', function () {
        db.all("select MAC, Product, FW_ver, CheckFW, Renew, SetupCode, SerialNumber, Date from Now", function (err, res) {
            if (!err) {
                dbString = JSON.stringify(res);
                console.log(dbString);
                count = 0;
                res.forEach(function (row) {
                    MAC[count] = row.MAC;
                    Product[count] = row.Product;
                    FW_ver[count] = row.FW_ver;
                    CheckFW[count] = row.CheckFW;
                    Renew[count] = row.Renew;
                    SetupCode[count] = row.SetupCode;
                    SerialNumber[count] = row.SerialNumber;
                    Time[count] = new Date(+row.Date).toLocaleString();
                    count++;
                });
            } else {
                console.log(err);
            }
        });
        db.all("select MAC, Product, FW_ver, CheckFW, Renew, SetupCode, SerialNumber, Date from Old", function (err, res) {
            if (!err) {
                dbString = JSON.stringify(res);
                console.log(dbString);
                count2 = 0;
                res.forEach(function (row) {
                    MAC2[count2] = row.MAC;
                    Product2[count2] = row.Product;
                    FW_ver2[count2] = row.FW_ver;
                    CheckFW2[count2] = row.CheckFW;
                    Renew2[count2] = row.Renew;
                    SetupCode2[count2] = row.SetupCode;
                    SerialNumber2[count2] = row.SerialNumber;
                    Time2[count2] = new Date(+row.Date).toLocaleString();
                    count2++;
                });
            } else {
                console.log(err);
            }
        });
        db.close();
        console.log('Close DB');
    });
    getNowTime(); //update time
};

function writeWeb(res, value){
    res.write('<tr>');
    res.write('<td>' + MAC[value] + '</td>');
    res.write('<td>' + Product[value] + '</td>');
    res.write('<td>' + FW_ver[value] + '</td>');
    res.write('<td>' + CheckFW[value] + '</td>');
    res.write('<td>' + Renew[value] + '</td>');
    res.write('<td>' + SetupCode[value] + '</td>');
    res.write('<td>' + SerialNumber[value] + '</td>');
    res.write('<td>' + Time[value] + '</td>');
    res.write('</tr>');
}

function writeWeb2(res, value){
    res.write('<tr>');
    res.write('<td>' + MAC2[value] + '</td>');
    res.write('<td>' + Product2[value] + '</td>');
    res.write('<td>' + FW_ver2[value] + '</td>');
    res.write('<td>' + CheckFW2[value] + '</td>');
    res.write('<td>' + Renew2[value] + '</td>');
    res.write('<td>' + SetupCode2[value] + '</td>');
    res.write('<td>' + SerialNumber2[value] + '</td>');
    res.write('<td>' + Time2[value] + '</td>');
    res.write('</tr>');
}

var server = http.createServer(function (req, res) {
    if (req.url == '/') {
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.write('<html><body>');
        res.write('<table id="myTableData" border="1">');
        res.write('<caption align="center">Ameba Server</caption>');

        res.write('<meta http-equiv="refresh" content="5">'); //Auto Update webpage

        res.write('<tr>');
        res.write('<td bgcolor="#FFD78C">MAC</td>');
        res.write('<td bgcolor="#FFD78C">Product</td>');
        res.write('<td bgcolor="#FFD78C">FW_ver</td>');
        res.write('<td bgcolor="#FFD78C">Check</td>');
        res.write('<td bgcolor="#FFD78C">Re-new</td>');
        res.write('<td bgcolor="#FFD78C">Setup Code</td>');
        res.write('<td bgcolor="#FFD78C">Serial Number</td>');
        res.write('<td bgcolor="#FFD78C">Updated</td>');
        res.write('</tr>');

        for(i = 0; i < count; i++)
            writeWeb(res, i);

        res.write('</table>');
        res.write('</body></html>');

        res.write("System time: " + SystemTime + "\n\n");
        res.end();
    } else if(req.url == '/Old'){
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.write('<html><body>');
        res.write('<table id="myTableData" border="2">');
        res.write('<caption align="center">Ameba Server Old</caption>');

        res.write('<meta http-equiv="refresh" content="10">'); //Auto Update webpage

        res.write('<tr>');
        res.write('<td bgcolor="#FFD78C">MAC</td>');
        res.write('<td bgcolor="#FFD78C">Product</td>');
        res.write('<td bgcolor="#FFD78C">FW_ver</td>');
        res.write('<td bgcolor="#FFD78C">Check</td>');
        res.write('<td bgcolor="#FFD78C">Re-new</td>');
        res.write('<td bgcolor="#FFD78C">Setup Code</td>');
        res.write('<td bgcolor="#FFD78C">Serial Number</td>');
        res.write('<td bgcolor="#FFD78C">Updated</td>');
        res.write('</tr>');

        for(i = 0; i < count2; i++)
            writeWeb2(res, i);

        res.write('</table>');
        res.write('</body></html>');

        res.write("System time: " + SystemTime + "\n\n");
        res.end();
    } else {
        res.end('Invalid Request!');
    }
});

console.log("DB Start");
setInterval(dbStart, 3000);

server.listen(port, hostname, () => {
    console.log('Server running at http://%s:%s/', hostname, port);
});