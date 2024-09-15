package mech.mania.starterpack.strategy;

import mech.mania.starterpack.game.BaseStrategy;
import mech.mania.starterpack.game.Plane;
import mech.mania.starterpack.game.PlaneType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// The following is the heart of your bot. This controls what your bot does.
// Feel free to change the behavior to your heart's content.
// You can also add other files under the strategy/ folder and import them

public class Strategy extends BaseStrategy {
    // BaseStrategy provides `this.team`, so you use `this.team` to see what team you are on

    // You can define whatever variables you want here
    private int myCounter = 0;
    private final Map<String, Double> mySteers = new HashMap<>();
    private final Random random = new Random();

    public Strategy(String team) {
        super(team);
    }

    @Override
    public Map<PlaneType, Integer> selectPlanes() {
        // Select which planes you want, and what number
        Map<PlaneType, Integer> planeSelection = new HashMap<>();
        planeSelection.put(PlaneType.BASIC, random.nextInt(6) + 5); // random between 5 and 10
        return planeSelection;
    }

    @Override
    public Map<String, Double> steerInput(Map<String, Plane> planes) {
        // Define a map to hold our response
        Map<String, Double> response = new HashMap<>();

        // For each plane
        for (Map.Entry<String, Plane> entry : planes.entrySet()) {
            String id = entry.getKey();
            Plane plane = entry.getValue();

            // We can only control our own planes
            if (!plane.getTeam().equals(this.team)) {
                // Ignore any planes that aren't our own - continue
                continue;
            }

            // If we're within the first 5 turns, just set the steer to 0
            if (myCounter < 5) {
                response.put(id, 0.);
            } else {
                // If we haven't initialized steers yet, generate a random one for this plane
                if (!mySteers.containsKey(id)) {
                    mySteers.put(id, random.nextDouble() * 2 - 1); // random value between -1 and 1
                }

                // Set the steer for this plane to our previously decided steer
                response.put(id, mySteers.get(id));
            }
        }

        // Increment counter to keep track of what turn we're on
        myCounter++;

        // Return the steers
        return response;
    }
}
