package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.rsapi.adapter.web.api.StatisticApi
import org.railwaystations.rsapi.adapter.web.api.StatisticApiDelegate
import org.springframework.stereotype.Controller
import java.util.*

@jakarta.annotation.Generated(
    value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
    comments = "Generator version: 7.7.0"
)
@Controller
class StatisticApiController2(
    @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: StatisticApiDelegate?
) : StatisticApi {
    private lateinit var delegate: StatisticApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : StatisticApiDelegate {})
    }

    override fun getDelegate(): StatisticApiDelegate = delegate
}
