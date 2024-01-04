# dl4s
Deep Learning For Search Code Examples <br ><br >
INTENDED TO BE RUN LOCALLY


# Setup

## Docker Containers
Create a `.env` file inside of the docker directory. Use the example from [Elastic Docker Compose](https://www.elastic.co/blog/getting-started-with-the-elastic-stack-and-docker-compose) <br ><br >
Add a variable to the `.env` file called DOCKER_BIND_MOUNT_LOCATION to a place on your local machine <br ><br >
Run `docker compose up --detach` from the `/docker` directory <br ><br ><br >

## Running Locally
Set the two environment variables in your terminal based on the `.env` variables in the `/docker` directory
* ELASTIC_PASSWORD
* DOCKER_BIND_MOUNT_LOCATION
<br ><br >

Pull the data from sources listed below into `/data/Songs`<br >
Ex. `/data/Songs/billboard` <br ><br >
Run `sbt run` from the `/songs` directory

### Synonyms
* Start by running the `CombineLyricSingleFile` program
* Run the `Word2VecGenerator` program to train the model
* Run the `CommandlineSynonymGenerator` program to see synonyms

### Suggestors
* Start by running the `LoadDataIntoElasticsearch` program
* Next, run the `CommandlineSuggestors` program to get suggestors

## Data source
http://millionsongdataset.com/
### Lyrics
https://lyrics.github.io/
https://github.com/kevinschaich/billboard