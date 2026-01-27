package server.morningcommit.config

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Configuration

@Configuration
@EnableFeignClients(basePackages = ["server.morningcommit.ai.client"])
class FeignConfig
