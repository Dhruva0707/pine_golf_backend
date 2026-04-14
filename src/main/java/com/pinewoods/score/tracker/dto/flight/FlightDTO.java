package com.pinewoods.score.tracker.dto.flight;

import java.util.Date;
import java.util.List;

public record FlightDTO(Long id, Date date, List<FlightScoreDTO> flights) {}
