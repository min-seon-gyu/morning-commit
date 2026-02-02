package server.morningcommit.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import server.morningcommit.service.TrackingService
import server.morningcommit.service.TrackingService.TrackResult
import java.net.URI

@RestController
class TrackingController(
    private val trackingService: TrackingService
) {

    @GetMapping("/track")
    fun track(@RequestParam url: String, @RequestParam subscriberId: Long): ResponseEntity<Void> {
        return when (val result = trackingService.trackClick(url, subscriberId)) {
            is TrackResult.Success -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.url))
                .build()
            TrackResult.InvalidUrl -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }
}
