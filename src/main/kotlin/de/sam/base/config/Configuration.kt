package de.sam.base.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.util.*

class Configuration {

    var host = "http://localhostt"
    var port = 7070
    var baseUrl: String = "http://localhost:$port"
    var enforceHost = false
    var name: String = "Base Template"
    var copyrightLine: String = "© Be a Lama, Inc. 2022. All rights reserved."
    var description: String = ""
    var passwordPepper: String = UUID.randomUUID().toString()
    var database = Database()
    var captcha = null as Captcha?
    var allowUserRegistration: Boolean = false
    var devEnvironment = true
    var logLevel = "TRACE"
    var fileTempDirectory = File("./upload/tmp").canonicalPath
    var tracking = Tracking()

    @JsonIgnore
    var version = "v0.0.4"

    class Database {
        var host: String = "localhost"
        var port: Int = 5432
        var username: String = "postgres"
        var password: String = "postgres"
        var database: String = "base"
    }

    class Captcha {
        var service: String = ""
        var siteKey: String = ""
        var secretKey: String = ""
        var locations: List<String> = arrayListOf("registration")
    }


    open class Tracking {
        var cronitor = Cronitor()
        var matomo = null as Matomo?

        class Cronitor {
            var url = ""
            var clientKey: String = ""
        }

        class Matomo {
            var url: String = ""
            var siteId: Int = 0
        }
    }

    companion object {
        var config = Configuration()
    }

    fun loadFromFile(file: File) {
        // load config from yaml file and parse with jackson
        val mapper = ObjectMapper(YAMLFactory())
        config = mapper.readValue(file, Configuration::class.java)
    }

    fun saveToFile(file: File) {
        // save config to yaml file and parse with jackson
        val mapper = ObjectMapper(YAMLFactory())
        //   mapper.enable(SerializationFeature.WRAP_ROOT_VALUE)
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        mapper.writeValue(file, this)
    }
}