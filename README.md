# syndication-server
A simple syndication server implementation that aggregates and distributes ATOM feeds

Compile the files
```bash
$javac -cp ".:./lib/*" -d ./ *.java
```

### Run automated test scripts

One content server, one GET client
```bash
$./test_single_client.sh
```

One content server, many GET clients
```bash
$./test_multiple_client.sh
```

### Test manually

Start the aggregation server
```bash
$java -cp ".:./lib/*" ds.assignment2.AggregationServer PORT &
```
where PORT is the port number that the server starts on

To start a client that sends GET request for atom.xml
```bash
$java -cp ".:./lib/*" ds.assignment2.GETClient HOST
```
where HOST is URL to the server of format
- "http://servername.domain.domain:portnumber"
- "http://servername:portnumber"
- "servername:portnumber"

The default value is 127.0.0.1:4567

To start a content server that sends PUT request to update atom.xml
```bash
$java -cp ".:./lib/*" ds.assignment2.ContentServer HOST PATH
```

where HOST is URL to the server of format
- "http://servername.domain.domain:portnumber"
- "http://servername:portnumber"
- "servername:portnumber"

and PATH is the path to the input file

To test a simple content server -> aggregation server -> client model
```bash
$java -cp ".:./lib/*" ds.assignment2.AggregationServer &
$java -cp ".:./lib/*" ds.assignment2.ContentServer 127.0.0.1:4567 input.txt
$java -cp ".:./lib/*" ds.assignment2.GETClient > output.txt
```
