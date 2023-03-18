#!/bin/bash

echo Starting aggregation server...
pkill -f "java"
java -cp ".:./lib/*" ds.assignment2.AggregationServer &
sleep 1

echo Testing PUT from one content server
echo Starting content server...
java -cp ".:./lib/*" ds.assignment2.ContentServer 127.0.0.1:4567 input.txt &
sleep 1

echo Testing GET from many clients
echo Starting GET client...
java -cp ".:./lib/*" ds.assignment2.GETClient > ./tests/output/single_client.txt &
sleep 1

echo Test completed
echo Comparing client 1 output with expected...
diff ./tests/output/single_client.txt ./tests/expected/single_client.txt >./tests/result/single_client.txt
echo Comparisons completed
echo Showing ./tests/result/single_client.txt
cat ./tests/result/single_client.txt
pkill -f "java"