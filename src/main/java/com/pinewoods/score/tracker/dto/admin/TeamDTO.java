package com.pinewoods.score.tracker.dto.admin;

import java.util.List;

public record TeamDTO(String name, List<PlayerDTO> players) {
}
