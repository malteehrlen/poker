lein uberjar
docker build -t poker .
docker save poker > poker.tar
scp poker.tar root@pokermancer.live:/root/
ssh root@pokermancer.live "docker load < poker.tar; docker stop \`docker ps -q\`"
ssh root@pokermancer.live "docker run -d -p 9190:9190 --restart=unless-stopped poker"

