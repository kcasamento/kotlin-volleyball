# Run

IP=$(docker inspect --format '{{ .NetworkSettings.IPAddress }}' consul); echo $IP

docker run --name=consul2 -e CONSUL_BIND_INTERFACE=eth0 -p 8500:8500 -p 8600:8600 consul -join=$IP

docker run -e CONSUL_BIND_INTERFACE=eth0 consul agent -join=$IP

ifconfig | grep -e "inet "

ipconfig getifaddr en0

https://blog.pcrisk.com/mac/12377-how-to-find-out-your-ip-address-on-mac


export HOST_IP=$(echo `ipconfig getifaddr en0`)
docker-compose up

https://github.com/wurstmeister/kafka-docker/wiki/Connectivity