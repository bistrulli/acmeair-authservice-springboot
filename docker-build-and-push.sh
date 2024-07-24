#!/bin/bash

IMAGE_NAME="rpizziol/acmeair-authservice-springboot"
TAG="0.13"


docker build -t $IMAGE_NAME:$TAG . && docker push $IMAGE_NAME:$TAG
