docker container stop icenlp
docker container rm icenlp
docker build . -t glaciersg/icenlp_api:latest
docker run -d --name=icenlp -p 8080:8080 glaciersg/icenlp_api:latest
