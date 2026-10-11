package com.mojang.authlib.services;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.services.response.NameAndId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Routes Minecraft profile searches through the launcher API. */
public class MinecraftServicesProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftServicesProfileRepository.class);

    // Keep the constructor signature used by MinecraftServicesDiscoveryService.
    public MinecraftServicesProfileRepository(Proxy proxy, MinecraftServicesDiscoveryService discoveryService) {}

    @Override
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
        Set<String> criteria = Arrays.stream(names)
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toSet());

        for (List<String> request : Iterables.partition(new ArrayList<>(criteria), 100)) {
            List<String> normalizedRequest = request.stream()
                    .map(MinecraftServicesProfileRepository::normalizeName)
                    .collect(Collectors.toList());
            Map<String, team.luxinfine.authlib.GameProfile> result =
                    MinecraftServicesSessionService.lookupProfiles(normalizedRequest, false);
            for (int i = 0; i < normalizedRequest.size(); i++) {
                String normalizedName = normalizedRequest.get(i);
                String requestedName = request.get(i);
                team.luxinfine.authlib.GameProfile profile = result.get(normalizedName);
                if (profile == null) {
                    LOGGER.debug("Couldn't find profile {}", requestedName);
                    callback.onProfileLookupFailed(requestedName,
                            new ProfileNotFoundException("Server did not find the requested profile"));
                } else {
                    LOGGER.debug("Successfully looked up profile {}", profile);
                    callback.onProfileLookupSucceeded(profile.name, profile.id);
                }
            }
        }
    }

    @Override
    public Optional<NameAndId> findProfileByName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        String normalizedName = normalizeName(name);
        Map<String, team.luxinfine.authlib.GameProfile> result =
                MinecraftServicesSessionService.lookupProfiles(Collections.singletonList(normalizedName), false);
        team.luxinfine.authlib.GameProfile profile = result.get(normalizedName);
        if (profile == null) {
            LOGGER.debug("Couldn't find profile {}", name);
            return Optional.empty();
        }
        return Optional.of(new NameAndId(profile.id, profile.name));
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
