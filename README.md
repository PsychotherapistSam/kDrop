# kDrop

This is the repository for the kDrop project.

## What is kDrop?

It is essentially a more lightweight alternative to the Nextcloud Filemanager, albeit much simpler than Nextcloud in
general (no calendar, contacts, syncing, extensions etc.). It is
a web application that allows you to share files with other people.

It was created because I only needed a simple file sharing application and Nextcloud was too heavy (and slow) for my
needs.

## How to install

### Requirements

- Java 18
- Postgres Database
- A web server (e.g. Apache, Nginx) for SSL Certificates

### Installation (Manual)

1. Download the latest release from the [releases page](https://github.com/PsychotherapistSam/kDrop/releases) and
   move the jar file to a directory of your
   choice.
2. Either copy the example `config.yml` file or run the jar file once to generate a new one.
3. (Optional) if you want SSL you need to use a reverse proxy like nginx or apache.

### Installation (Docker Compose)
_This will be added soon, as the image is still uploaded to my forgejo instance, but here's a sample docker-compose.yml
You can build the docker image yourself by using the Dockerfile in the repository._

```yaml
version: "3.8"
services:
  app:
    image: TBD
    ports:
      - 7070:7070
    restart: unless-stopped
    depends_on:
      - db
    volumes:
      - ./logs:/app/logs
      - ./config.yml:/app/config.yml
      - ./files:/app/files
      - ./temp:/app/temp # can be omitted
  db:
    image: postgres:13
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
    restart: unless-stopped
    volumes:
      - ./db:/var/lib/postgresql/data
```


## How to use

### Creating an Admin account

The first registered user will automatically be assigned the Admin role

## How to build

### Requirements

- Java 18
- Gradle

### Building

1. Clone the repository
2. Run `gradle build shadowJar`
3. The jar file will be in `build/libs`

## How to contribute

If you want to contribute, please create a pull request. If you want to report a bug or have a question, please create
an issue. Keep in mind that this is, for now, just a pet project of mine.

## License

This project is licensed under
the [GNU AGPLv3](https://github.com/PsychotherapistSam/kDrop/blob/master/LICENSE).