package dev.flagpole.api.flag;

import dev.flagpole.api.flag.FlagDtos.CreateFlagRequest;
import dev.flagpole.api.flag.FlagDtos.FlagResponse;
import dev.flagpole.api.flag.FlagDtos.UpdateFlagRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectKey}/flags")
@RequiredArgsConstructor
public class FlagController {

    private final FlagService flagService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public FlagResponse create(@PathVariable String projectKey, @Valid @RequestBody CreateFlagRequest request) {
        return flagService.create(projectKey, request);
    }

    @GetMapping
    public List<FlagResponse> list(@PathVariable String projectKey,
                                   @RequestParam(defaultValue = "false") boolean includeArchived) {
        return flagService.list(projectKey, includeArchived);
    }

    @GetMapping("/{flagKey}")
    public FlagResponse get(@PathVariable String projectKey, @PathVariable String flagKey) {
        return flagService.get(projectKey, flagKey);
    }

    @PatchMapping("/{flagKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public FlagResponse update(@PathVariable String projectKey, @PathVariable String flagKey,
                               @Valid @RequestBody UpdateFlagRequest request) {
        return flagService.update(projectKey, flagKey, request);
    }

    @DeleteMapping("/{flagKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public void archive(@PathVariable String projectKey, @PathVariable String flagKey) {
        flagService.archive(projectKey, flagKey);
    }
}
