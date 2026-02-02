package server.morningcommit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "click_log",
    indexes = [Index(name = "idx_click_log_target_url", columnList = "targetUrl")]
)
class ClickLog(
    @Column(nullable = false)
    val subscriberId: Long,

    @Column(nullable = false)
    val targetUrl: String,

    @Column(nullable = false)
    val clickedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
