package com.pinewoods.score.tracker.dto.admin;

public class AuthenticationDTOs {
    public record AuthRequestDTO(String username, String password){}
    public record AuthResponseDTO(String token){}
}
