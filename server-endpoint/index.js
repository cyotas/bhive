var http = require('http')
var mysql = require('mysql')

//api
const port = 3033
const sql = "SELECT * FROM information"

// sql conneciton
var conneciton = mysql.createConnection({
  host: "localhost",
  port: 3306,
  database: "BIENEN",
  user: "J2",
  password: "J2"
});

var server = http.createServer(function (request, response) {
    console.log(request.method)
    switch (request.url) {
        case '/':
            response.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
            loadData()
            response.write(loadData())
            response.end()
            break
        default:
            response.end('Invalid Request')
    }
})

//run
server.listen(port)
console.log('Server is running...')
console.log('localhost:' + port)

function loadData() {
    conneciton.query(sql, function (err, result) {
      if (err) throw err
      console.log(result)
      return result.toString()
    });
}
  
conneciton.connect(function (err) {
    if (err) throw err
    console.log('Connected to database')
  });