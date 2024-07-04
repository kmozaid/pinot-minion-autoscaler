#### Build and Push docker image
```shell
./mvnw spring-boot:build-image
docker tag docker.io/library/pinot-minion-autoscaler:0.1.0 008096229222.dkr.ecr.us-west-2.amazonaws.com/dev/wap-pinot-minion-autoscaler:0.1.0
docker push 008096229222.dkr.ecr.us-west-2.amazonaws.com/dev/wap-pinot-minion-autoscaler:0.1.0
```