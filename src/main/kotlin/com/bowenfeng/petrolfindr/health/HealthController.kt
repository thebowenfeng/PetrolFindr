package com.bowenfeng.petrolfindr.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/healthcheck")
    suspend fun healthCheck(): String = "OK"
}
