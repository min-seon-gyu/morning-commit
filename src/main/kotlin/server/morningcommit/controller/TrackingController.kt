package server.morningcommit.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import server.morningcommit.service.TrackingService
import java.net.URI

@RestController
class TrackingController(
    private val trackingService: TrackingService
) {

    @GetMapping("/track")
    fun track(@RequestParam url: String, @RequestParam subscriberId: Long): ResponseEntity<Void> {
        val redirectUrl = trackingService.trackClick(url, subscriberId)

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl))
            .build()
    }
}
