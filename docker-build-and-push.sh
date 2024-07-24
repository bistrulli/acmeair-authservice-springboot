#!/bin/bash

IMAGE_NAME="rpizziol/acmeair-authservice-springboot"
TAG="0.16"


docker build -t $IMAGE_NAME:$TAG . && docker push $IMAGE_NAME:$TAG
