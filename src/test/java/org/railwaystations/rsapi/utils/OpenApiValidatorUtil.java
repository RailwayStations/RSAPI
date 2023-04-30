package org.railwaystations.rsapi.utils;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;

public class OpenApiValidatorUtil {

    public static ResultMatcher validOpenApiResponse() {
        return openApi().isValid(OpenApiInteractionValidator
                .createFor("static/openapi.yaml")
                .withWhitelist(ValidationErrorsWhitelist
                        .create()
                        .withRule("Ignore requests", WhitelistRules.isRequest())
                )
                .build());
    }

}
