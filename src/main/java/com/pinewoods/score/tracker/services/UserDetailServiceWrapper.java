package com.pinewoods.score.tracker.services;

import com.pinewoods.score.tracker.dao.PlayerRepository;
import com.pinewoods.score.tracker.entities.Player;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceWrapper implements UserDetailsService {
    private final PlayerRepository playerRepo;

    public UserDetailServiceWrapper(PlayerRepository repo) {
        this.playerRepo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepo.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Player not found"));

        return User.withUsername(player.getName())
                .password(player.getPassword()) // already encoded
                .roles(player.getRole().name()) // "ADMIN" or "PLAYER"
                .build();
    }
}
