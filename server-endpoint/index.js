var http = require('http')
var util = require('util')
var mysql = require('mysql')
const { url } = require('inspector')

//api
const port = 3033
const select = "SELECT * FROM %s"
const tables = "SHOW TABLES"

// sql connection
var connection = mysql.createConnection({
    host: "localhost",
    port: 3306,
    database: "bhive",
    user: "J2",
    password: "J2"
});

var server = http.createServer(function (request, response) {
    if (request.method == "GET" && request.url != "/") {
        var url = request.url.slice(1)
        checkIfTableExists(url, (exists) => {
            if(!exists) response.end('Invalid Request')

            response.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })

            loadData(url, (result) => {
                response.write(result)
                response.end()
            })  
        })
    } else response.end('Invalid Request')
})

//run
server.listen(port)
console.log('Server is running...')
console.log('localhost:' + port)

function checkIfTableExists(url, callback) {
    connection.query(tables, function (err, result) {
        if (err) throw err
        exists = JSON.stringify(result).includes(url)
        return callback (exists)
    })
}

function loadData(url, callback) {
    connection.query(tables, function (err, result) {
        if (err) throw err
        if (JSON.stringify(result).includes(url)) {
            connection.query(util.format(select, url), function (err, result) {
                if (err) throw err
                result = JSON.stringify(result.map(v => Object.assign({}, v)))
                return callback(result)
            });
        }
    });
}

connection.connect(function (err) {
    if (err) throw err
    console.log('Connected to database')
});