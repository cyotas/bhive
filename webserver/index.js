var http = require('http')
var fs = require('fs')

//web server
const port = 2022

var server = http.createServer(function (request, response) {
    switch (request.url) {
        case '/':
            sendFile('./index.html', response)
            break
        default:
            response.end('Invalid Request')
    }
})

function sendFile(path, response) {
    response.writeHead(200, { 'Content-Type': 'text/' + path.substr(path.lastIndexOf('.') + 1) })
    response.write(fs.readFileSync(path))
    response.end()
}

//run
server.listen(port)
console.log('Server is running...')
console.log('localhost:' + port)