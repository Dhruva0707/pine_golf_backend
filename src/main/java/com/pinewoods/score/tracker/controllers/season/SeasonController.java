package com.pinewoods.score.tracker.controllers.season;

import com.pinewoods.score.tracker.controllers.admin.utilities.ControllerUtilities;
import com.pinewoods.score.tracker.dto.season.SeasonDTO;
import com.pinewoods.score.tracker.dto.season.TeamStandingDTO;
import com.pinewoods.score.tracker.dto.tournament.TournamentDTO;
import com.pinewoods.score.tracker.entities.season.Season;
import com.pinewoods.score.tracker.entities.season.TeamStanding;
import com.pinewoods.score.tracker.entities.tournament.Tournament;
import com.pinewoods.score.tracker.services.season.SeasonService;
import com.pinewoods.score.tracker.services.tournament.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/seasons")
@RequiredArgsConstructor
@Tag(name = "Season Management", description = "Endpoints for managing the golfing season lifecycle")
public class SeasonController {

    private final SeasonService seasonService;
    private final TournamentService tournamentService;

    // ==================== Create Season ====================
    @PostMapping("/start")
    @Operation(summary = "Start a new golfing season",
            description = "Creates a new season and initializes the tournament.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Season created"),
            @ApiResponse(responseCode = "404", description = "Season already exists")
    })
    ResponseEntity<SeasonDTO> createSeason(@RequestBody String name) {
        SeasonDTO season = seasonService.createSeason(name).toDto();
        URI resultUri = ControllerUtilities.createResourceURI("name", season.name());
        return ResponseEntity.created(resultUri).body(season);
    }

    // ==================== Read Season ====================
    @GetMapping
    @Operation(summary = "Get all seasons")
    ResponseEntity<List<String>> getAllSeasons() {
        List<Season> seasons = seasonService.getAllSeasons();
        return ResponseEntity.ok(seasons.stream()
                .map(Season::getName)
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details about a season")
    ResponseEntity<SeasonDTO> getSeason(@PathVariable("id") String seasonName) {
        return ResponseEntity.ok(seasonService.getSeasonByName(seasonName).toDto());
    }

    @GetMapping("/{id}/tournaments")
    @Operation(summary = "Get all tournaments in a season")
    ResponseEntity<List<TournamentDTO>> getTournaments(@PathVariable("id") String seasonName) {
        return ResponseEntity.ok(seasonService.getTournaments(seasonName).stream()
                .map(Tournament::toDTO)
                .toList());
    }

    @GetMapping("{id}/standing")
    @Operation(summary = "Get season standing of each team")
    ResponseEntity<List<TeamStandingDTO>> getStandings(@PathVariable("id") String seasonName) {
        return ResponseEntity.ok(seasonService.getStandings(seasonName).stream()
                .map(TeamStanding::toDTO)
                .toList());
    }

    // ==================== Update Season ====================
    @PostMapping("/{seasonName}/finish")
    @Operation(summary = "Finish a season and get season standing")
    ResponseEntity<List<TeamStandingDTO>> finishSeason(@PathVariable("seasonName") String seasonName) {
        Season season = seasonService.finishSeason(seasonName);

        return ResponseEntity.ok(
                season.getStandings().stream()
                        .sorted(Comparator.comparingInt(TeamStanding::getPoints).reversed())
                        .map(TeamStanding::toDTO)
                .toList()
        );
    }

    // ==================== Delete Season ====================
    @DeleteMapping("/{seasonName}")
    @Operation(summary = "Delete a season")
    ResponseEntity<Void> deleteSeason(@PathVariable("seasonName") String seasonName) {
        seasonService.deleteSeason(seasonName);
        return ResponseEntity.noContent().build();
    }
}
