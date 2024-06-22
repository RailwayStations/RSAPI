package org.railwaystations.rsapi.adapter.web.controller

import org.railwaystations.openapi.api.CountriesApiDelegate
import java.util.*

@jakarta.annotation.Generated(
    value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
    comments = "Generator version: 7.6.0"
)
//@Controller
class CountriesApi2Controller(
    @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: CountriesApiDelegate?
) : CountriesApi2 {
    private lateinit var delegate: CountriesApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : CountriesApiDelegate {})
    }

    override fun getDelegate(): CountriesApiDelegate = delegate
}
