package com.pinewoods.score.tracker.dto.flight;

import java.util.Date;
import java.util.List;

public record FlightDTO(Date date, List<FlightScoreDTO> flights) {}
