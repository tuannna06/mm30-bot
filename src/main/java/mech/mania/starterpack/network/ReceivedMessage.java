package mech.mania.starterpack.network;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ReceivedMessage(boolean isZombie, String phase, ObjectNode message) {}
