#!/bin/sh

#curl http://0.0.0.0:8080/IceNLPWeb/process -H 'Content-Type: application/json' -d '{"type":"text", "content":"hæ"}'

LOC=/process/service
#LOC=/IceNLPWeb/process/parse

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d '{"type":"text", "content":"Gamli lkenvf maðurinn nkeng borðar sfg3en kalda lveosf súpu nveo með aþ mjög aa góðri lveþsf lyst nveþ . ."}'
echo


echo "### Error ###"
TEST='{}'
curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d $TEST
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d ''
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d 'lskdjflkj'
echo
