package org.railwaystations.rsapi.utils

import com.atlassian.oai.validator.OpenApiInteractionValidator.createFor
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist.create
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules
import org.springframework.test.web.servlet.ResultMatcher

object OpenApiValidatorUtil {
    @JvmStatic
    fun validOpenApiResponse(): ResultMatcher {
        return openApi().isValid(
            createFor("static/openapi.yaml")
                .withWhitelist(
                    create()
                        .withRule("Ignore requests", WhitelistRules.isRequest())
                )
                .build()
        )
    }
}
