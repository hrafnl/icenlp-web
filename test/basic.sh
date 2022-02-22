#!/bin/sh

#curl http://0.0.0.0:8080/IceNLPWeb/process -H 'Content-Type: application/json' -d '{"type":"text", "content":"hæ"}'

#LOC=/process/service
LOC=/IceNLPWeb/process

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d '{"type":"text", "content":"hæ"}'
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d '{"type":"text", "content":"hæ, hvað seigir þú gott. Nei."}'
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d '{"type":"text", "content":"Gamli maðurinn borðar kalda súpu með mjög góðri lyst."}'
echo

echo "### Error ###"
TEST='{}'
curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d $TEST
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d ''
echo

curl http://0.0.0.0:8080$LOC -H 'Content-Type: application/json' -d 'lskdjflkj'
echo
