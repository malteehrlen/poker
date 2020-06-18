lein uberjar
docker build -t poker .
docker save poker > poker.tar
scp poker.tar root@pokermancer.live:/root/

