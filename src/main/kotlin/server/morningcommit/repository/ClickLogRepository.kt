package server.morningcommit.repository

import org.springframework.data.jpa.repository.JpaRepository
import server.morningcommit.domain.ClickLog

interface ClickLogRepository : JpaRepository<ClickLog, Long>
