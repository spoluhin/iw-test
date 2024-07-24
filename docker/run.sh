cd ..
mvn clean install -DskipTests=true
cd -
cp ../target/*jar ./app.jar
docker-compose build
docker-compose up -d
docker logs -f test-app
