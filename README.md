# icenlp
IceNLP is an open source Natural Language Processing (NLP) toolkit for analysing and processing Icelandic text. It includes tools for tokenisation, lemmatisation, PoS tagging and shallow parsing.

# Getting started
In the root directory you must run `ant` in order to build the project and then you run `./run.sh` to build the docker image and run it.

# API calls
All the API calls use post and input/outputs are in a json format, following the specification from [elg](https://european-language-grid.readthedocs.io/en/stable/all/A3_API/LTInternalAPI.html#basic-api-pattern).

| HTTP METHOD | Description |
| ----------- | --------------- |
| /IceNLPWeb/process/nlp | Takes in an article in icelandic and then returns the article summary |
| /IceNLPWeb/process/parse | Takes in POS labeled Icelandic text and returns an output according to a shallow syntactic annotation scheme |

# Testing

In the test folder can be found basic scripts for testing the functionality of the api.

## Testing elg

For testing if the [elg specifications](https://european-language-grid.readthedocs.io/en/stable/all/A3_API/LTInternalAPI.html#basic-api-pattern) are being met, you must specify which api you want to test in the `.env` file. Then you run `docker-compose up` and submit the api calls to `localhost:8080/process/service`

# Funding
This ELG API was developed by the [language and voice labs](https://lvl.ru.is/) at [Reykjavík University](https://en.ru.is/) in EU's CEF project: [Microservices at your service](https://www.lingsoft.fi/en/microservices-at-your-service-bridging-gap-between-nlp-research-and-industry).

# European Language Grid
The docker image is hosted [here](https://hub.docker.com/r/glaciersg/icenlp_api) and is running on the european language grid as [IceNLP (running on elg)](https://live.european-language-grid.eu/catalogue/tool-service/17684) and [IceParser - Shallow parser](https://live.european-language-grid.eu/catalogue/tool-service/17682) using the two api calls respectively.

# Underlying tool
The underlying toolkit is [iceNLP-web](https://github.com/hrafnl/icenlp-web) by [Hrafn Loftsson](https://github.com/hrafnl). Code from a separate [branch](https://github.com/hrafnl/icenlp-web/tree/icenlp-elg) in the original GitHub repository for the tool is used when the docker image is built. 
