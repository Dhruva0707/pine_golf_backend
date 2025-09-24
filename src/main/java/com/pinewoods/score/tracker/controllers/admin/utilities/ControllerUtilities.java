package com.pinewoods.score.tracker.controllers.admin.utilities;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;

public class ControllerUtilities {

    public static URI createResourceURI(String pathVariableName, Object value) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/{" + pathVariableName + "}")
                .buildAndExpand(value)
                .toUri();
    }
}
