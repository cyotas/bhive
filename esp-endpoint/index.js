var http = require('http')
var mysql = require('mysql')

//constants
const port = 4044
const sql = "INSERT INTO information (in_temp, in_humi, out_temp, out_humi) VALUES ?"

// sql connection
var connection = mysql.createConnection({
    host: "localhost",
    port: 3306,
    database: "BIENEN",
    user: "J2",
    password: "J2"
})

// http server
var server = http.createServer(function (request, response) {
    if (request.method == 'POST') {

        var json_data = ''
        request.on('data', (data) => {
            json_data += data
        })

        request.on('end', () => {
            var json = JSON.parse(json_data)
            storeData([[json["in_temp"], json["in_humi"], json["out_temp"], json["out_humi"]]])
            response.writeHead(200)
            response.end()
        })
    }
})

// store data in db
function storeData(data) {
    connection.query(sql, [data], function (err) {
        if (err) throw err
    })
}

//run
server.listen(port)
console.log('Server is running...')
console.log('port:' + port)

connection.connect(function (err) {
    if (err) throw err
    console.log('Connected to database')
})