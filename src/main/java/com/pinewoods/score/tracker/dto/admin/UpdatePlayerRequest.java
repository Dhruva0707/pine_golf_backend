package com.pinewoods.score.tracker.dto.admin;

public record UpdatePlayerRequest(
        String newName,
        String password
) {}

