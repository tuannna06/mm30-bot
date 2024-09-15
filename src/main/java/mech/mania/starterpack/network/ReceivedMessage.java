package mech.mania.starterpack.network;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ReceivedMessage(Phase phase, ObjectNode data) {}
