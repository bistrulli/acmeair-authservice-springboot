#!/bin/bash

IMAGE_NAME="rpizziol/acmeair-authservice-springboot"
TAG="0.17"


docker build --no-cache -t $IMAGE_NAME:$TAG . && docker push $IMAGE_NAME:$TAG
