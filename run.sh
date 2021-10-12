docker container stop icenlp
docker container rm icenlp
docker build . -t icenlp:example
docker run -d --name=icenlp -p 8080:8080 icenlp:example
