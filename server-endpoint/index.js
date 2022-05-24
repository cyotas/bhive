var http = require('http')
var mysql = require('mysql')

//api
const port = 3033
const sql = "SELECT * FROM information"

// sql connection
var connection = mysql.createConnection({
    host: "localhost",
    port: 3306,
    database: "BIENEN",
    user: "J2",
    password: "J2"
});

var server = http.createServer(function (request, response) {
    if (request.method == "GET") {
        switch (request.url) {
            case '/':
                response.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
                loadData((result) => {
                    response.write(result)
                    response.end()
                })
                break
            default:
                response.end('Invalid Request')
        }
    } else response.end('Invalid Request')
})

//run
server.listen(port)
console.log('Server is running...')
console.log('localhost:' + port)

function loadData(callback) {
    connection.query(sql, function (err, result) {
        if (err) throw err
        result = JSON.stringify(result.map(v => Object.assign({}, v)))
        return callback(result)
    });
}

connection.connect(function (err) {
    if (err) throw err
    console.log('Connected to database')
});