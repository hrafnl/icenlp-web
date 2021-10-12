# -*- coding: utf-8 -*-
import requests
import json 

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
inp = str(params).replace('\'', '"')
print(inp)
print(json_object)
r = requests.post("http://localhost:8080/IceNLPWeb/process", json=inp)
print(r.text)
