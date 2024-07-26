cd ..
gradle jar
cd -
cp ../build/libs/*SNAPSHOT.jar ./app.jar
docker-compose build
docker-compose up -d
docker logs -f test-app
