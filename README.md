# dl4s
Deep Learning For Search Code Examples


# Setup

## ElasticSearch
https://www.elastic.co/guide/en/elasticsearch/reference/8.11/docker.html
`docker network create elastic`
`docker pull docker.elastic.co/elasticsearch/elasticsearch:8.11.3`
`docker run --name es01 --net elastic -p 9200:9200 -it -m 1GB docker.elastic.co/elasticsearch/elasticsearch:8.11.3`


## Data source
http://millionsongdataset.com/