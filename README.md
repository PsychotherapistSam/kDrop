# Kopimi/Share

This is the repository for the Kopimi/Share project.

## What is Kopimi/Share?

It is essentially a more lightweight alternative to the Nextcloud Filemanager, albeit much simpler than Nextcloud in
general (no calendar, contacts, syncing, extensions etc.). It is
a web application that allows you to share files with other people.

It was created because I only needed a simple file sharing application and Nextcloud was too heavy (and slow) for my
needs.

## How to install

### Requirements

- Java 11
- Postgres Database
- A web server (e.g. Apache, Nginx) for SSL Certificates

### Installation (Manual)

1. Download the latest release from the [releases page](https://github.com/PsychotherapistSam/kopimi-share/releases) and
   move the jar file to a directory of your
   choice.
2. Either copy the example `config.yml` file or run the jar file once to generate a new one.
3. (Optional) if you want SSL you need to use a reverse proxy like nginx or apache.

### Installation (Docker)

_This will be added soon_

## How to use

### Creating an Admin account

As there currently is no setup page, you need to register an account on the website and then change the `roles` column
in the Database to `ADMIN`.

## How to build

### Requirements

- Java 11
- Gradle

### Building

1. Clone the repository
2. Run `gradle build shadowJar`
3. The jar file will be in `build/libs`

## How to contribute

If you want to contribute, please create a pull request. If you want to report a bug or have a question, please create
an issue.

## License

This project is licensed under
the [GNU AGPLv3](https://github.com/PsychotherapistSam/kopimi-share/blob/master/LICENSE).