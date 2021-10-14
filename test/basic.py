# -*- coding: utf-8 -*-
import requests
import json 

def run(json):
    inp = str(json).replace('\'', '"')
    return requests.post("http://localhost:8080/IceNLPWeb/process", json=inp)

params = {
    'query':'hello',
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
json.loads(r.text)
print(r.text)

r = run({})
json.loads(r.text)
print(r.text)

r = run({"query":"Hæ, ég er svangur. Hvað með þig?"})
print(r.text)
json.loads(r.text)
