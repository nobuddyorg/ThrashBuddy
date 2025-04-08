package de.besessener.cloudthrash

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class CloudthrashApplication {

    static void main(String[] args) {
        SpringApplication.run(CloudthrashApplication, args)
    }

}
