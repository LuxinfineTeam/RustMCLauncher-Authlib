package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.luxinfine.authlib.CommonAPI;

import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger();

    public YggdrasilGameProfileRepository(Proxy proxy, Environment environment) {}

    @Override
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
        Set<String> criteria = Sets.newHashSet();

        for (String name : names) {
            if (!Strings.isNullOrEmpty(name))
                criteria.add(name.toLowerCase());
        }

        for (List<String> request : Iterables.partition(criteria, 100)) {
            Map<String, team.luxinfine.authlib.GameProfile> result = CommonAPI.lookupProfiles(request, false);

            for (String name : request) {
                team.luxinfine.authlib.GameProfile profile = result.get(name);
                if (profile == null) {
                    LOGGER.debug("Couldn't find profile {}", name);
                    callback.onProfileLookupFailed(name, new ProfileNotFoundException("Server did not find the requested profile"));
                } else {
                    GameProfile mcProfile = YggdrasilMinecraftSessionService.convertProfile(profile);
                    LOGGER.debug("Successfully looked up profile {}", mcProfile);
                    callback.onProfileLookupSucceeded(mcProfile);
                }
            }
        }
    }
}
