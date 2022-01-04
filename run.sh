docker container stop icenlp
docker container rm icenlp
docker build . -t glaciersg/icenlp:v1.0
docker run -d --name=icenlp -p 8080:8080 glaciersg/icenlp:v1.0
