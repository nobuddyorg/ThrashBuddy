package org.nobuddy.thrashbuddy

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ThrashBuddyApplication {

    static void main(String[] args) {
        SpringApplication.run(ThrashBuddyApplication, args)
    }

}
