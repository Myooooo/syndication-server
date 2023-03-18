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
echo Starting GET client 1...
java -cp ".:./lib/*" ds.assignment2.GETClient > ./tests/output/multiple_client1.txt &
echo Starting GET client 2...
java -cp ".:./lib/*" ds.assignment2.GETClient > ./tests/output/multiple_client2.txt &
echo Starting GET client 3...
java -cp ".:./lib/*" ds.assignment2.GETClient > ./tests/output/multiple_client3.txt &
sleep 1

echo Test completed
echo Comparing client 1 output with expected...
diff ./tests/output/multiple_client1.txt ./tests/expected/multiple_client1.txt >./tests/result/multiple_client1.txt
echo Comparing client 2 output with expected...
diff ./tests/output/multiple_client2.txt ./tests/expected/multiple_client2.txt >./tests/result/multiple_client2.txt
echo Comparing client 3 output with expected...
diff ./tests/output/multiple_client3.txt ./tests/expected/multiple_client3.txt >./tests/result/multiple_client3.txt
echo Comparisons completed
echo Showing ./tests/result/multiple_client1.txt
cat ./tests/result/multiple_client1.txt
echo Showing ./tests/result/multiple_client2.txt
cat ./tests/result/multiple_client2.txt
echo Showing ./tests/result/multiple_client3.txt
cat ./tests/result/multiple_client3.txt
pkill -f "java"