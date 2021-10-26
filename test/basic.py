# -*- coding: utf-8 -*-
import requests
import json 

def run(json):
    inp = str(json).replace('\'', '"')
    return requests.post("http://localhost:8080/IceNLPWeb/process", json=inp)

params = {
    'query':'hæ, ég heiti haldór',
    'stricktTokenize':'true',
    'inputTokenize':3,
    'tagger':'IceTagger',
    'sentline':'true',
    'markunknown':'true',
    'functions':'true',
    'phraseline':'true',
    'submit':'Analyse'
        }

json_object = json.dumps(params) 
r = run(params)
print("INPUT: "+str(params))
print(r.text)
#print("OUTPUT: "+r.text.encode('utf-8').decode('unicode-escape'))
json.loads(r.text)

r = run({})
print("INPUT: "+str({}))
print("OUTPUT: "+r.text)
json.loads(r.text)

inp = {"query":"Hæ, ég er svangur. Hvað með þig?"}
r = run(inp)
print("INPUT: "+str(inp))
print("OUTPUT: "+r.text)
json.loads(r.text)
